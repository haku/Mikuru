package com.vaguehope.mikuru;

import android.widget.ScrollView;
import android.widget.TextView;

public class ConsoleAppender {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final TextView tv;
	private final ScrollView sv;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public ConsoleAppender (TextView tv, ScrollView sv) {
		this.tv = tv;
		this.sv = sv;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void append (String... msgs) {
		for (String msg : msgs) {
			this.tv.append(msg);
		}
		this.sv.smoothScrollTo(0, this.tv.getHeight());
	}
	
	public void clear () {
		this.tv.setText("");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
