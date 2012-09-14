/*
 *
 * Copyright (c) 2009-2012,
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import domain.CodeElement;
import flow.InflowAnalysis;
import flow.types.FlowType;
import flow.types.ParameterFlow;


/**
 * CallArgSourceSpecs represent sources that are arguments to another function.
 * 
 * For example, if code you analyze invokes a function {@code foo(Object obj)}
 * and foo <em>writes</em> to the argument, then {@code obj} would be a source.
 * 
 */
public class CallArgSourceSpec extends SourceSpec {
	final String name = "CallArgSource";
	public CallArgSourceSpec(MethodNamePattern name, int[] args) {
		namePattern = name;
		argNums = args;
	}

	@Override
	public<E extends ISSABasicBlock> void addDomainElements(
			Map<BasicBlockInContext<E>, Map<FlowType<E>, Set<CodeElement>>> taintMap,
			IMethod target, BasicBlockInContext<E> block, SSAInvokeInstruction invInst,
			int[] newArgNums, ISupergraph<BasicBlockInContext<E>, CGNode> graph, PointerAnalysis pa, CallGraph cg) {

		for (int j = 0; j<newArgNums.length; j++) {
			for (FlowType ft:getFlowType(block,invInst,block.getNode(), target, newArgNums[j], pa)) {
				InflowAnalysis.addDomainElements(taintMap, block, ft, 
				        CodeElement.valueElements(pa, block.getNode(), invInst.getUse(newArgNums[j])));
			}
		}
	}


	public<E extends ISSABasicBlock> Collection<FlowType<E>> getFlowType(
	        BasicBlockInContext<E> block,
	        SSAInvokeInstruction invInst,
			CGNode node, IMethod target, int argNum, PointerAnalysis pa) {
		HashSet<FlowType<E>> flowSet = new HashSet<FlowType<E>>();
		flowSet.clear();
		for(int i: argNums) {
		    flowSet.add(new ParameterFlow<E>(block, i, true));
		}
		return flowSet;
	}
}
