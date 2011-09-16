/*
 * Copyright 2011 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
