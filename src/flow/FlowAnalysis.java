/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
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

package flow;

import static util.MyLogger.LogLevel.DEBUG;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import synthMethod.MethodAnalysis;
import util.AndroidAppLoader;
import util.CLI;
import util.GraphUtil;
import util.IFDSTaintFlowFunctionProvider;
import util.MyLogger;

import com.google.common.collect.Lists;
import com.ibm.wala.dataflow.IFDS.IFlowFunctionMap;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationProblem;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.IFDS.TabulationSolver;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;

import domain.CodeElement;
import domain.DomainElement;
import domain.IFDSTaintDomain;
import flow.types.FlowType;

public class FlowAnalysis {

    public static <E extends ISSABasicBlock>
    TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> 
    analyze(final AndroidAppLoader<E> loader,
          Map<BasicBlockInContext<E>,
          Map<FlowType<E>,Set<CodeElement>>> initialTaints,
          IFDSTaintDomain<E> d,
          MethodAnalysis<E> methodAnalysis
          ) throws CancelRuntimeException {
        return analyze(loader.graph, loader.cg, loader.pa, initialTaints, d, methodAnalysis);
    }
    
    
    public static <E extends ISSABasicBlock>
      TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> 
      analyze(final ISupergraph<BasicBlockInContext<E>, CGNode> graph,
              CallGraph cg,
              PointerAnalysis pa,
              Map<BasicBlockInContext<E>,
              Map<FlowType<E>,Set<CodeElement>>> initialTaints,
              IFDSTaintDomain<E> d,
              MethodAnalysis<E> methodAnalysis
            ) throws CancelRuntimeException {

        System.out.println("*************************");
        System.out.println("* Running flow analysis *");
        System.out.println("*************************");

        final IFDSTaintDomain<E> domain = d;

        final List<PathEdge<BasicBlockInContext<E>>>
           initialEdges = Lists.newArrayList();

        //Add PathEdges to the taints
        for(Entry<BasicBlockInContext<E>, Map<FlowType<E>,Set<CodeElement>>> e:initialTaints.entrySet())
        {
            for(Entry<FlowType<E>,Set<CodeElement>> e2:e.getValue().entrySet())
            {
                for(CodeElement o:e2.getValue())
                {
                	BasicBlockInContext<E>[] bbic = graph.getEntriesForProcedure(e.getKey().getNode());
                	for (int i = 0; i < bbic.length; i++) {
                		//Add PathEdge <s_p,0> -> <n,d1>
                		initialEdges.add(PathEdge.createPathEdge(bbic[i], 0, e.getKey(), domain.getMappedIndex(new DomainElement(o,e2.getKey()))));
                	}
                    //initialEdges.add(PathEdge.createPathEdge(e.getKey(), 0, e.getKey(), domain.getMappedIndex(new DomainElement(o,e2.getKey()))));
                }
            }
        }
        //Add PathEdges to the entry points of the supergraph <s_main,0> -> <s_main,0>
        for (CGNode entry : cg.getEntrypointNodes()) {
        	BasicBlockInContext<E>[] bbic = graph.getEntriesForProcedure(entry);
        	for (int i = 0; i < bbic.length; i++)
        		initialEdges.add(PathEdge.createPathEdge(bbic[i], 0, bbic[i], 0));
        }

        final IFlowFunctionMap<BasicBlockInContext<E>> functionMap =
            new IFDSTaintFlowFunctionProvider<E>(domain, graph, pa, methodAnalysis);
        
        final TabulationProblem<BasicBlockInContext<E>, CGNode, DomainElement>
          problem =
            new TabulationProblem<BasicBlockInContext<E>, CGNode, DomainElement>() {

            public TabulationDomain<DomainElement, BasicBlockInContext<E>> getDomain() {
                return domain;
            }

            public IFlowFunctionMap<BasicBlockInContext<E>> getFunctionMap() {
                return functionMap;
            }

            public IMergeFunction getMergeFunction() {
                return null;
            }

            public ISupergraph<BasicBlockInContext<E>, CGNode> getSupergraph() {
                return graph;
            }

            public Collection<PathEdge<BasicBlockInContext<E>>> initialSeeds() {
                return initialEdges;
//              CGNode entryProc = cfg.getCallGraph().getEntrypointNodes()
//                      .iterator().next();
//              BasicBlockInContext<ISSABasicBlock> entryBlock = cfg
//                      .getEntry(entryProc);
//              for (int i = 0; i < entryProc.getIR().getNumberOfParameters(); i++) {
//                  list.add(PathEdge.createPathEdge(entryBlock, 0, entryBlock,
//                          domain.getMappedIndex(new LocalElement(i + 1))));
//              }
//              return list;
            }

        };
        TabulationSolver<BasicBlockInContext<E>, CGNode, DomainElement> solver =
            TabulationSolver.make(problem);

        try {
        	TabulationResult<BasicBlockInContext<E>,CGNode, DomainElement> flowResult = solver.solve();
        	if (CLI.hasOption("IFDS-Explorer")) {
        		for (int i = 1; i < domain.getSize(); i++) {        			
                    MyLogger.log(DEBUG,"DomainElement #"+i+" = " + domain.getMappedObject(i));        			
        		}
        		GraphUtil.exploreIFDS(flowResult);
        	}
            return flowResult;
        } catch (CancelException e) {
            throw new CancelRuntimeException(e);
        }
    }

}
