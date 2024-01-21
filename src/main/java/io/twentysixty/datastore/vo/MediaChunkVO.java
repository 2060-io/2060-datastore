package io.twentysixty.datastore.vo;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MediaChunkVO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8630758413105035092L;
	
	private String data;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	
		
	
	
}
