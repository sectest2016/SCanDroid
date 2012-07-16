/*
 *
 * Copyright (c) 2010-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>)
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package spec;

import spec.ISourceSpec.SourceType;
import spec.ISinkSpec.SinkType;

public class Specs implements ISpecs {
	static String act = "Landroid/app/Activity";
	static String svc = "Landroid/app/Service";
	static String prv = "Landroid/content/ContentProvider";
	static String rslv = "Landroid/content/ContentResolver";
	static String ctx = "Landroid/content/Context";
	static String http = "Landroid/net/AndroidHttpClient";
	static String bnd = "Landroid/os/IBinder";

	static MethodNamePattern actCreate =
			new MethodNamePattern(act, "onCreate");
	static MethodNamePattern actStart =
			new MethodNamePattern(act, "onStart");
	static MethodNamePattern actResume =
			new MethodNamePattern(act, "onResume");
	static MethodNamePattern actStop =
			new MethodNamePattern(act, "onStop");
	static MethodNamePattern actRestart =
			new MethodNamePattern(act, "onRestart");
	static MethodNamePattern actDestroy =
			new MethodNamePattern(act, "onDestroy");
	static MethodNamePattern actOnActivityResult =
			new MethodNamePattern(act, "onActivityResult");

	static MethodNamePattern actOnRestoreInstanceState =
			new MethodNamePattern(act, "onRestoreInstanceState");
	static MethodNamePattern actOnSaveInstanceState =
			new MethodNamePattern(act, "onSaveInstanceState");

	static MethodNamePattern actSetResult =
			new MethodNamePattern(act, "setResult");
	
	static MethodNamePattern actGetIntent =
			new MethodNamePattern(act, "getIntent");

	static MethodNamePattern svcCreate =
			new MethodNamePattern(svc, "onCreate");
	static MethodNamePattern svcStart =
			new MethodNamePattern(svc, "onStart");
	static MethodNamePattern svcStartCommand =
			new MethodNamePattern(svc, "onStartCommand");
	static MethodNamePattern svcBind =
			new MethodNamePattern(svc, "onBind");
	// static MethodNamePattern svcTransact =
	//     new MethodNamePattern(svc, "onTransact");

	static MethodNamePattern rslvQuery =
			new MethodNamePattern(rslv, "query");
	static MethodNamePattern rslvInsert =
			new MethodNamePattern(rslv, "insert");
	static MethodNamePattern rslvUpdate =
			new MethodNamePattern(rslv, "update");

	static MethodNamePattern prvCreate =
			new MethodNamePattern(prv, "onCreate");
	static MethodNamePattern prvQuery =
			new MethodNamePattern(prv, "query");
	static MethodNamePattern prvInsert =
			new MethodNamePattern(prv, "insert");
	static MethodNamePattern prvUpdate =
			new MethodNamePattern(prv, "update");

	static MethodNamePattern ctxStartActivity =
			new MethodNamePattern(ctx, "startActivity");
	static MethodNamePattern ctxStartService =
			new MethodNamePattern(ctx, "startService");
	static MethodNamePattern ctxBindService =
			new MethodNamePattern(ctx, "bindService");

	static MethodNamePattern bndTransact =
			new MethodNamePattern(bnd, "transact");
	static MethodNamePattern bndOnTransact =
			new MethodNamePattern(bnd, "onTransact");    

	static MethodNamePattern httpExecute =
			new MethodNamePattern(http, "execute");

	static MethodNamePattern[] entrypointSpecs = {
		actCreate,
		actStart,
		actResume,
		actStop,
		actRestart,
		actDestroy,
		actOnActivityResult,

		svcCreate,
		svcStart,
		svcStartCommand,
		svcBind,
		//svcTransact,

		prvCreate,
		prvQuery,
		prvInsert,
		prvUpdate,
	};
	public MethodNamePattern[] getEntrypointSpecs() { return entrypointSpecs; }

	static SourceSpec[] sourceSpecs = {
		new EntryArgSourceSpec( actCreate, null ),
		//doesn't have any parameters
		// new EntryArgSourceSpec( actStart, null ),
		// new EntryArgSourceSpec( actResume, null ),
		// new EntryArgSourceSpec( actStop, null ),
		// new EntryArgSourceSpec( actRestart, null ),
		// new EntryArgSourceSpec( actDestroy, null ),
		//track all parameters?  or just the Intent data(3)
		new EntryArgSourceSpec( actOnActivityResult, null ),
		new EntryArgSourceSpec( actOnRestoreInstanceState, null ),
		new EntryArgSourceSpec( actOnSaveInstanceState, null ),

		new EntryArgSourceSpec( svcCreate, null ),
		new EntryArgSourceSpec( svcStart, null ),
		new EntryArgSourceSpec( svcStartCommand, null ),
		new EntryArgSourceSpec( svcBind, null ),
		//doesn't exist
		// new EntryArgSourceSpec( svcTransact, null ),

		//no parameters
		//new EntryArgSourceSpec( prvCreate, null ),
		new CallArgSourceSpec( prvQuery, null ),
		new CallArgSourceSpec( prvInsert, null ),
		new CallArgSourceSpec( prvUpdate, null ),
		
		new EntryArgSourceSpec(bndOnTransact, new int[] { 2 }, SourceType.BINDER_SOURCE),
		
		new CallArgSourceSpec(bndTransact, new int[] { 3 }, SourceType.BINDER_SOURCE),
		
		new CallRetSourceSpec(rslvQuery, new int[] { 1 }),
		new CallRetSourceSpec(httpExecute, new int[] {}),
		new CallRetSourceSpec(actGetIntent, new int[] {}),
		
		
		new CallRetSourceSpec(new MethodNamePattern("LTest/Apps/GenericSource", "getIntSource"), new int[]{}),

		
	};
	
	public SourceSpec[] getSourceSpecs() { return sourceSpecs; }

	static SinkSpec[] sinkSpecs = {
		new CallArgSinkSpec(actSetResult, new int[] { 2 }, SinkType.RETURN_SINK),
		new CallArgSinkSpec(bndTransact, new int[] { 2 }),
		new CallArgSinkSpec(rslvQuery, new int[] { 2, 3, 4, 5 }, SinkType.PROVIDER_SINK),
		new CallArgSinkSpec(rslvInsert, new int[] { 2 }, SinkType.PROVIDER_SINK),
		new CallArgSinkSpec(rslvUpdate, new int[] { 2 }, SinkType.PROVIDER_SINK),
		new CallArgSinkSpec(ctxBindService, new int[] { 1 }, SinkType.SERVICE_SINK),
		new CallArgSinkSpec(ctxStartService, new int[] { 1 }, SinkType.SERVICE_SINK),
		new CallArgSinkSpec(ctxStartActivity, new int[] { 1 }, SinkType.ACTIVITY_SINK),

		
		new EntryArgSinkSpec( bndOnTransact, new int[] { 3 } ),
		new EntryArgSinkSpec( actOnActivityResult, new int[] { 2 } ),
		new EntryArgSinkSpec( actOnSaveInstanceState, new int[] { 0 } ),

		//new EntryRetSinkSpec(prvQuery),
		
		new CallArgSinkSpec(new MethodNamePattern("LTest/Apps/GenericSink", "setSink"), new int[]{ 1 }),

	};

	public SinkSpec[] getSinkSpecs() { return sinkSpecs; }
}
