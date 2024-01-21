package io.twentysixty.datastore.util;

import io.twentysixty.datastore.enums.StatusCode;
import io.twentysixty.datastore.vo.ErrorVO;

public class ApiError {

	
	/*public static ErrorBuilder statusCode(StatusCode sc) {
		ErrorBuilder builder = new ErrorBuilder(sc);
		return builder;
	}
	*/
	public static ErrorBuilder message(String message) {
		ErrorBuilder builder = new ErrorBuilder(message);
		return builder;
	}
	
	
	
	public static class ErrorBuilder {
		
		
		//private StatusCode statusCode;
		private String message;
		
		public ErrorBuilder(String message) {
			this.message = message;
		}

		/*public ErrorBuilder(StatusCode sc) {
			this.statusCode = sc;
		}

		public ErrorBuilder statusCode(StatusCode sc) {
			this.statusCode = sc;
			return this;
		}
		*/
		public ErrorBuilder message(String message) {
			this.message = message;
			return this;
		}
		
		/*public StatusCode getStatusCode() {
			return statusCode;
		}

		public void setStatusCode(StatusCode statusCode) {
			this.statusCode = statusCode;
		}
*/
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
		
		public ErrorVO build() {
			ErrorVO vo = new ErrorVO();
			//vo.setStatusCode(statusCode);
			vo.setMessage(message);
			return vo;
		}
	}
}
