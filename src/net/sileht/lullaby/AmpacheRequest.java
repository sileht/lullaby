package net.sileht.lullaby;
/* Copyright (c) 20010 ABAAKOUK Mehdi  <theli48@gmail.com>
*
* +------------------------------------------------------------------------+
* | This program is free software; you can redistribute it and/or          |
* | modify it under the terms of the GNU General Public License            |
* | as published by the Free Software Foundation; either version 2         |
* | of the License, or (at your option) any later version.                 |
* |                                                                        |
* | This program is distributed in the hope that it will be useful,        |
* | but WITHOUT ANY WARRANTY; without even the implied warranty of         |
* | MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          |
* | GNU General Public License for more details.                           |
* |                                                                        |
* | You should have received a copy of the GNU General Public License      |
* | along with this program; if not, write to the Free Software            |
* | Foundation, Inc., 59 Temple Place - Suite 330,                         |
* | Boston, MA  02111-1307, USA.                                           |
* +------------------------------------------------------------------------+
*/
import java.util.ArrayList;

import net.sileht.lullaby.R;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public abstract class AmpacheRequest extends Handler {

	protected Activity mCurrentActivity;
	protected String[] mDirective;
	protected int mMax;
	protected boolean mQuick;
	
	private boolean stop = false;
	
	private static final String TAG = "LullabyBackendRequest";

	public AmpacheRequest(Activity activity, String[] directive) {
		this(activity, directive, false);
	}
	public AmpacheRequest(Activity activity, String[] directive, Boolean quick) {
		super();
		mCurrentActivity = activity;
		mDirective = directive;
		mQuick = quick;
	}
	public void stop(){
		stop = true;
	}
	public void send(){
		Message requestMsg = new Message();
		requestMsg.what = 1;
		requestMsg.arg1 = 0;
		requestMsg.obj = mDirective;
		requestMsg.replyTo = new Messenger(this);
		Log.d(TAG, "Request send for "+mDirective[0]);
		if (mQuick){
			Lullaby.comm.incomingRequestHandler.sendMessageAtFrontOfQueue(requestMsg);
		} else {
			Lullaby.comm.incomingRequestHandler.sendMessage(requestMsg);
		}
		showProgress();
	}
	
	public abstract void add_objects(ArrayList<?> list);

	private void hideProgress(){
		if (mCurrentActivity != null){
			ProgressBar p = (ProgressBar) mCurrentActivity.findViewById(R.id.progress);
			p.setVisibility(View.INVISIBLE);
		}		
	}
	private void showProgress(){
		if (mCurrentActivity != null){
			ProgressBar p = (ProgressBar) mCurrentActivity.findViewById(R.id.progress);
			p.setVisibility(View.VISIBLE);
		}
	}

	public void handleMessage(Message msg) {
		if (stop)
			return;
		switch (msg.what) {
		case 1:
			add_objects((ArrayList<?>) msg.obj);

			/* queue up the next inc */
			if (((ArrayList<?>) msg.obj).size() >= 100) {
				Message requestMsg = new Message();
				requestMsg.obj = mDirective;
				requestMsg.what = 1;
				requestMsg.arg1 = msg.arg1 + 100;
				requestMsg.replyTo = new Messenger(this);
				Lullaby.comm.incomingRequestHandler
						.sendMessage(requestMsg);

				showProgress();
			} else {
				hideProgress();
			}
			break;
		case 100:
			hideProgress();
			if (mCurrentActivity != null){
				Toast.makeText(mCurrentActivity,
						"Ampache not configured.\nCheck your settings.",
						Toast.LENGTH_LONG).show();
			}
			Message requestMsg = new Message();
			requestMsg.obj = mDirective;
			requestMsg.what = 1;
			requestMsg.arg1 = msg.arg1;
			requestMsg.replyTo = new Messenger(this);
			Lullaby.comm.incomingRequestHandler
					.sendMessage(requestMsg);
			break;
		case 9999:
			/* handle an error */
			if (mCurrentActivity != null){
				Toast.makeText(
						mCurrentActivity, "Communicator error:" + (String) msg.obj,
						Toast.LENGTH_LONG).show();
			}
			break;
		}
	}
}
