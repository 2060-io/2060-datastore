package io.twentysixty.datastore.enums;


public enum StatusCode {
	
	
	OK("OK"),
	PERMISSION_DENIED("Permission Denied"),
	BAD_ARGUMENT("Bad Argument"),
	NULL_ARGUMENT("Null Argument"),
	INTEGRITY_ERROR("Integrity Error"),
	SERVER_ERROR("Server Error"),
	
	
	;

		private String msg;
		
		private StatusCode(String msg){
			this.msg = msg;
			
		}

		public String getMsg() {
			return msg;
		}
		/*
		private int index;

		public int getIndex(){
			return this.index;
		}

		public static StatusCode getEnum(Integer index){
			if (index == null)
		return null;

			switch(index){
				case 0: return BASIC_INFORMATION;
				case 1: return CIPHERING;
				case 2: return CUSTOM_FIELDS;
				case 3: return SLEEPY_FEATURES;
				default: return null;
			}
		}
		
		public String getValue() {
			return this.name();
		}
		private String label;
		public String getLabel() {
			return label;
		}
		
		private String description;
		public String getDescription() {
			return description;
		}
		
		public String getName() {
			return this.toString();
		}
*/
	
}
