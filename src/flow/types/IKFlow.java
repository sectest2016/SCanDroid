/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
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

package flow.types;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

public class IKFlow implements FlowType {
    public final InstanceKey ik;
    private final CGNode inNode;
    private final int argNum;
    private final String type;
    private final String callee;
    private final int ikIndex;

    public IKFlow(InstanceKey ik)
    {
        this.ik = ik;
        inNode = null;
        type = "";
        argNum = 0;
        callee = "";
        ikIndex = -1;
    }
    
    public IKFlow(InstanceKey ik, int index, CGNode node, String type, String callee)
    {
        this.ik = ik;
        this.inNode = node;
        this.type = type;
        this.argNum = 0;
        this.callee = callee;
        this.ikIndex = index;
    }
    
    public IKFlow(InstanceKey ik, int index, CGNode node, String type, String callee, int argNum)
    {
        this.ik = ik;
        this.inNode = node;
        this.type = type;
        this.argNum = argNum;
        this.callee = callee;
        this.ikIndex = index;
    }

    @Override
    public int hashCode()
    {
        return ik.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if(other == null)
            return false;
        if(other instanceof IKFlow)
            return ((IKFlow)other).ik.equals(ik);
        return false;
    }

    @Override
    public String toString()
    {
    	if (argNum == 0)
    		return type+" - IKFlow(Caller:"+inNode.getMethod().getSignature()+" ==> Callee:"+callee+") IK: " + ik + " index: " + ikIndex;
    	else
    		return type+" - IKFlow(Caller:"+inNode.getMethod().getSignature()+" ==> Callee:"+callee+", Parameter:"+argNum+") IK: " + ik + " index: " + ikIndex;
//        return "IKFlow("+ik+")";
    }

	@Override
	public CGNode getRelevantNode() {
		return inNode;
	}
	
	public InstanceKey getIK() {
		return ik;
	}
}
