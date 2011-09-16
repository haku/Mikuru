package com.vaguehope.mikuru;

import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class ConsoleAppender implements Appender {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final TextView tv;
	protected final ScrollView sv;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public ConsoleAppender (TextView tv, ScrollView sv) {
		this.tv = tv;
		this.sv = sv;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void append (String... msgs) {
		for (String msg : msgs) this.tv.append(msg);
		this.sv.post(this.scrollDown);
	}
	
	private final Runnable scrollDown = new Runnable() {
		@Override
		public void run() {
			ConsoleAppender.this.sv.fullScroll(View.FOCUS_DOWN);
		}
	};
	
	public void clear () {
		this.tv.setText("");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
