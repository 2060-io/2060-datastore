package io.twentysixty.datastore.vo;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MediaVO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8630758413005035092L;
	
	//@Required
	private UUID id;
    
	//@Required
	//@Validator(minSize=1)
	private Integer chunks;
	private Boolean complete;
	private Instant createdTs;
	private Instant expireTs;
	
	
	
	
	
	public Boolean getComplete() {
		return complete;
	}
	public void setComplete(Boolean complete) {
		this.complete = complete;
	}
	public Instant getCreatedTs() {
		return createdTs;
	}
	public void setCreatedTs(Instant createdTs) {
		this.createdTs = createdTs;
	}
	public Instant getExpireTs() {
		return expireTs;
	}
	public void setExpireTs(Instant expireTs) {
		this.expireTs = expireTs;
	}
	
	
	public Integer getChunks() {
		return chunks;
	}
	public void setChunks(Integer chunks) {
		this.chunks = chunks;
	}
	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	
	
	
	
}
