package com.vaguehope.mikuru;

public interface RsyncService {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isRunning ();
	
	public void cancelRun ();
	
	public void addAppender (Appender appender);
	
	public void removeAppender (Appender appender);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
