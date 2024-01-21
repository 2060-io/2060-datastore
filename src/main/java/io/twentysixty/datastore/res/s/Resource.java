package io.twentysixty.datastore.res.s;

import java.io.File;

import javax.ws.rs.FormParam;

public class Resource {

    @FormParam("chunk")
    public File chunk;
}