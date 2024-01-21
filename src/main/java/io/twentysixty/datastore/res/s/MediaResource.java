package io.twentysixty.datastore.res.s;



import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.twentysixty.datastore.enums.StatusCode;
import io.twentysixty.datastore.exc.InvalidChunkNumberException;
import io.twentysixty.datastore.exc.MediaAlreadyExistsException;
import io.twentysixty.datastore.exc.MissingTokenException;
import io.twentysixty.datastore.exc.NoTokenForMediaException;
import io.twentysixty.datastore.exc.PermissionDeniedException;
import io.twentysixty.datastore.exc.UnknownMediaException;
import io.twentysixty.datastore.svc.MediaService;
import io.twentysixty.datastore.util.ApiError;
import io.twentysixty.datastore.vo.ErrorVO;


@Path("")
public class MediaResource  {

	
	private static final Logger logger = Logger.getLogger(MediaResource.class);

	
	@ConfigProperty(name = "io.twentysixty.datastore.media.maxchunks")
	Long maxChunks;
	
	@Inject MediaService mediaService;
	
	@ConfigProperty(name = "io.twentysixty.datastore.debug")
	Boolean debug;
	
	
	
	@GET
	@Path("/r/{uuid}")
	@Operation(summary = "Serve media content",
			description = "Serve media content as MediaType.APPLICATION_OCTET_STREAM")
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments and returned ErrorVO.", 
				content = { @Content( schema=@Schema(implementation = ErrorVO.class)) }
		),
		@APIResponse(responseCode = "404", description = "Not found."),
		@APIResponse(responseCode = "200", description = "OK.")
		
	})
	public Response render( @Parameter(description = "UUID of Media", required=true) @PathParam(value = "uuid") UUID uuid) {
		
		if (uuid == null) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("uuid")).build();
		}
		File media = null;
		try {
			media = mediaService.getMediaFile(uuid);
		} catch (Exception e) {
			return Response.status(Status.NOT_FOUND).build();
		}
		
		return Response.status(Status.OK).entity(media).type(MediaType.APPLICATION_OCTET_STREAM).build();
    }
	
	@DELETE
	@Path("/d/{uuid}")
	@Operation(summary = "Delete a Media by specifying its uuid and management Token",
			description = "Delete a Media by specifying its uuid and management Token")
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments and returned ErrorVO.", 
				content = { @Content( schema=@Schema(implementation = ErrorVO.class)) }
		),
		@APIResponse(responseCode = "404", description = "Not found."),
		@APIResponse(responseCode = "200", description = "OK.")
		
	})
	public Response delete( @Parameter(description = "UUID of Media", required=true) @PathParam(value = "uuid") UUID uuid,
			@Parameter(description = "Management Token of Media", required=true) @QueryParam(value = "token") String token) {
		
		if (uuid == null) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("uuid")).build();
		}
		if ((token == null) || (token.isBlank())) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("uuid")).build();
		}
		
		
		try {
			mediaService.deleteMedia(uuid, token);
		} catch (PermissionDeniedException e) {
			return Response.status(Status.FORBIDDEN).entity(ApiError.message(e.getMessage())).build();
		} catch (UnknownMediaException e) {
			return Response.status(Status.NOT_FOUND).entity(ApiError.message(e.getMessage())).build();
		} catch (MissingTokenException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message(e.getMessage())).build();
		} catch (NoSuchAlgorithmException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiError.message(e.getMessage())).build();
		} catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiError.message(e.getMessage())).build();
		} catch (NoTokenForMediaException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("media " + uuid + " has no management token")).build();

		}
		
		return Response.status(Status.OK).build();
    }
	
	@PUT
	@POST
	@Path("/c/{uuid}/{noc}")
	@Operation(summary = "Create or update a Media by specifying its uuid and noc (number of chunks) and an optional management token",
			description = "Create or update a Media by specifying its uuid and noc (number of chunks) and an optional management token (required for update)"
			)
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments and returned ErrorVO.", 
				content = { @Content( schema=@Schema(implementation = ErrorVO.class)) }
				),
		@APIResponse(responseCode = "403", description = "Permission Denied.", 
		content = { @Content( schema=@Schema(implementation = ErrorVO.class)) }
				),
		@APIResponse(responseCode = "500", description = "Server error, please check logs.", 
		content = { @Content( schema=@Schema(implementation = ErrorVO.class)) }
				),
		@APIResponse(responseCode = "201", description = "Media successfully created."),
		
		@APIResponse(responseCode = "204", description = "Media successfully updated and ready to receive new chunks.")
		})
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createOrUpdate(@Parameter(description = "UUID of Media", required=true) @PathParam(value = "uuid") UUID uuid, 
			@Parameter(description = "Number of Chunks", required=true) @PathParam(value = "noc") Integer numberOfChunks,
			@Parameter(description = "Optional Management Token", required=false) @QueryParam(value = "token") String token) {
		
		if (uuid == null) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("uuid")).build();
		}
		if (numberOfChunks == null) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("noc")).build();
		}
		
		if (numberOfChunks>maxChunks) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("maxChunks: " + maxChunks)).build();
		}
		boolean update = false;
		// MediaAlreadyExistsException, PermissionDeniedException, IOException 
		try {
			update = mediaService.createOrUpdateTmpMedia(uuid, numberOfChunks, token);
		} catch (MediaAlreadyExistsException e) {
			logger.error("", e);
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("media " + uuid + " already exists")).build();
		} catch (MissingTokenException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message(e.getMessage())).build();
		} catch (PermissionDeniedException e) {
			return Response.status(Status.FORBIDDEN).entity(ApiError.message(e.getMessage())).build();
		} catch (NoSuchAlgorithmException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiError.message(e.getMessage())).build();
		} catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiError.message(e.getMessage())).build();
		} catch (NoTokenForMediaException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("media " + uuid + " has no management token")).build();

		}
		if (update) {
			return Response.status(Status.NO_CONTENT).build();
		} else {
			return Response.status(Status.CREATED).build();
		}
		
		
    }

	@PUT
	@POST
	@Path("/u/{uuid}/{c}")
	
	@Operation(summary = "Upload Chunk number c for a Media",
			description = "Upload Chunk number c for a Media")
	@APIResponses({
		@APIResponse(responseCode = "400", description = "Check arguments and returned ErrorVO.", 
				content = { @Content( schema=@Schema(implementation = ErrorVO.class)) }
		),
		@APIResponse(responseCode = "403", description = "Permission Denied."),
		@APIResponse(responseCode = "500", description = "Server error, please retry."),
		
		@APIResponse(responseCode = "201", description = "Chunk successfully uploaded.") }
)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadChunk( @Parameter(description = "UUID of Media", required=true) @PathParam(value = "uuid") UUID uuid, 
    		@Parameter(description = "Chunk Number", required=true) @PathParam(value = "c") Integer partNumber,
    		@Parameter(description = "Optional Management Token", required=false) @QueryParam(value = "token") String token,
    		@MultipartForm Resource data) {
		
		if (uuid == null) {
			logger.info("uploadChunk: null uuid");
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("id")).build();
		}
		if (partNumber == null) {
			logger.info("uploadChunk: null part number");
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("partNumber")).build();
		}
		if (data == null) {
			logger.info("uploadChunk: null data");
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("data")).build();
		}
		if (data.chunk == null) {
			logger.info("uploadChunk: null chunk");
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("chunk")).build();
		}
		try {
			mediaService.copyChunkToTmpRepo(uuid, partNumber, data.chunk, token);
		} catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiError.message(e.getMessage())).build();

		} catch (InvalidChunkNumberException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message(e.getMessage())).build();

		} catch (UnknownMediaException e) {
			//if (debug) logger.info("", e);
			return Response.status(Status.NOT_FOUND).entity(ApiError.message(e.getMessage())).build();

		} catch (NoSuchAlgorithmException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ApiError.message(e.getMessage())).build();
		} catch (NoTokenForMediaException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message("media " + uuid + " has no management token")).build();

		} catch (MissingTokenException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message(e.getMessage())).build();

		} catch (MediaAlreadyExistsException e) {
			return Response.status(Status.BAD_REQUEST).entity(ApiError.message(e.getMessage())).build();

		} catch (PermissionDeniedException e) {
			return Response.status(Status.FORBIDDEN).entity(ApiError.message("you do not have permission to manipulate media " + uuid)).build();

		}
		
		return Response.status(Status.CREATED).build();
		
    }

	

}