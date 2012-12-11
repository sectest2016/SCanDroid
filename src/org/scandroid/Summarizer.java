/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
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
package org.scandroid;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.scandroid.domain.CodeElement;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.flow.FlowAnalysis;
import org.scandroid.flow.InflowAnalysis;
import org.scandroid.flow.OutflowAnalysis;
import org.scandroid.flow.functions.TaintTransferFunctions;
import org.scandroid.flow.types.FieldFlow;
import org.scandroid.flow.types.FlowType;
import org.scandroid.flow.types.FlowType.FlowTypeVisitor;
import org.scandroid.flow.types.IKFlow;
import org.scandroid.flow.types.ParameterFlow;
import org.scandroid.flow.types.ReturnFlow;
import org.scandroid.flow.types.StaticFieldFlow;
import org.scandroid.spec.ISpecs;
import org.scandroid.spec.StaticSpecs;
import org.scandroid.synthmethod.DefaultSCanDroidOptions;
import org.scandroid.synthmethod.TestSpecs;
import org.scandroid.synthmethod.XMLSummaryWriter;
import org.scandroid.util.AndroidAnalysisContext;
import org.scandroid.util.CGAnalysisContext;
import org.scandroid.util.IEntryPointSpecifier;
import org.scandroid.util.ThrowingSSAInstructionVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.traverse.DFSPathFinder;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.StringStuff;

/**
 * @author acfoltzer
 *
 * @param <E>
 */
/**
 * @author acfoltzer
 *
 * @param <E>
 */
/**
 * @author acfoltzer
 * 
 * @param <E>
 */
public class Summarizer<E extends ISSABasicBlock> {
	private static final Logger logger = LoggerFactory
			.getLogger(Summarizer.class);

	private static final long TIME_LIMIT = 60 * 60;
	public static final String WALA_NATIVES_XML = "data/MethodSummaries.xml";

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassHierarchyException
	 * @throws ParserConfigurationException
	 * @throws URISyntaxException
	 * @throws CancelException
	 * @throws IllegalArgumentException
	 */
	public static void main(String[] args) throws ClassHierarchyException,
			IOException, ParserConfigurationException,
			IllegalArgumentException, CancelException, URISyntaxException {

		if (args.length < 2) {
			logger.error("Usage: Summarizer <jarfile> <methoddescriptor> [static|notstatic]");
			logger.error("   methoddescriptor -- a specification of a java method, formatted as:");
			logger.error("                       some.package.Clasas(Ljava/lang/String;I)Ljava/lang/String;");
			System.exit(1);
		}

		String appJar = args[0];
		String methoddescriptor = args[1];
		// boolean isStatic = false;
		// if (args[2].equals("static")) {
		// isStatic = true;
		// }

		Summarizer<IExplodedBasicBlock> s = new Summarizer<IExplodedBasicBlock>(
				appJar);
		s.summarize(methoddescriptor, null);

		System.out.println(s.serialize());
	}

	private final AndroidAnalysisContext analysisContext;
	private XMLSummaryWriter writer;

	public Summarizer(final String appJar) throws IllegalArgumentException,
			ClassHierarchyException, IOException, CancelException,
			URISyntaxException, ParserConfigurationException {
		analysisContext = new AndroidAnalysisContext(
				new DefaultSCanDroidOptions() {
					@Override
					public URI getClasspath() {
						return new File(appJar).toURI();
					}

					@Override
					public boolean stdoutCG() {
						return false;
					}
				});
		writer = new XMLSummaryWriter();
	}

	public void summarize(String methodDescriptor)
			throws ClassHierarchyException, CallGraphBuilderCancelException,
			IOException, ParserConfigurationException {
		summarize(methodDescriptor, new TimedMonitor(TIME_LIMIT));
	}

	public void summarize(String methodDescriptor, IProgressMonitor monitor)
			throws ClassHierarchyException, CallGraphBuilderCancelException,
			IOException, ParserConfigurationException {

		MethodReference methodRef = StringStuff
				.makeMethodReference(methodDescriptor);

		Collection<IMethod> entryMethods = analysisContext.getClassHierarchy()
				.getPossibleTargets(methodRef);

		if (entryMethods.size() > 1) {
			logger.error("More than one imethod found for: " + methodRef);
		} else if (entryMethods.size() == 0) {
			logger.error("No method found for: " + methodRef);
		}

		final IMethod imethod = entryMethods.iterator().next();

		MethodSummary summary = new MethodSummary(methodRef);
		summary.setStatic(imethod.isStatic());

		CGAnalysisContext<IExplodedBasicBlock> cgContext = new CGAnalysisContext<IExplodedBasicBlock>(
				analysisContext, new IEntryPointSpecifier() {
					@Override
					public List<Entrypoint> specify(
							AndroidAnalysisContext analysisContext) {
						return Lists
								.newArrayList((Entrypoint) new DefaultEntrypoint(
										imethod, analysisContext
												.getClassHierarchy()));
					}
				});

		Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> dfAnalysis = runDFAnalysis(
				cgContext, summary, monitor);
		logger.debug(dfAnalysis.toString());

		List<SSAInstruction> instructions = new MethodSummarizer(cgContext, imethod).summarizeFlows(dfAnalysis);

		if (0 == instructions.size()) {
			logger.warn("No instructions in summary for " + methodDescriptor);
			return;
		}

		for (SSAInstruction inst : instructions) {
			summary.addStatement(inst);
		}

		writer.add(summary);
	}

	/**
	 * Generate XML for these summaries.
	 * 
	 * @return
	 */
	public String serialize() {
		logger.debug("Generated summary:\n{}", writer.serialize());
		return writer.serialize();
	}

	public Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> runDFAnalysis(
			CGAnalysisContext<IExplodedBasicBlock> cgContext,
			MethodSummary mSummary) throws ClassHierarchyException,
			CallGraphBuilderCancelException, IOException {
		return runDFAnalysis(cgContext, mSummary, new TimedMonitor(TIME_LIMIT));
	}

	public Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> runDFAnalysis(
			CGAnalysisContext<IExplodedBasicBlock> cgContext,
			MethodSummary mSummary, IProgressMonitor monitor)
			throws IOException, ClassHierarchyException,
			CallGraphBuilderCancelException {

		ISpecs specs = TestSpecs.combine(new MethodSummarySpecs(mSummary),
				new StaticSpecs(cgContext.getClassHierarchy(), mSummary
						.getMethod().getSignature()));

		Map<BasicBlockInContext<IExplodedBasicBlock>, Map<FlowType<IExplodedBasicBlock>, Set<CodeElement>>> initialTaints = InflowAnalysis
				.analyze(cgContext, new HashMap<InstanceKey, String>(), specs);

		System.out.println("  InitialTaints count: " + initialTaints.size());

		IFDSTaintDomain<IExplodedBasicBlock> domain = new IFDSTaintDomain<IExplodedBasicBlock>();
		TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult = FlowAnalysis
				.analyze(cgContext, initialTaints, domain, monitor,
						new TaintTransferFunctions<IExplodedBasicBlock>(domain,
								cgContext.graph, cgContext.pa, true));

		Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> permissionOutflow = new OutflowAnalysis(
				cgContext, specs).analyze(flowResult, domain);

		return permissionOutflow;
	}

	/**
	 * Eventually, we'd like these pointer keys to encompass the entire
	 * environment (such as static fields) in scope for this method. For now,
	 * though, parameters suffice.
	 * 
	 * @param method
	 * @return
	 */
	public Set<PointerKey> getInputPointerKeys(
			CGAnalysisContext<IExplodedBasicBlock> cgContext, IMethod method) {
		CGNode node = cgContext.nodeForMethod(method);
		Set<PointerKey> pkSet = Sets.newHashSet();
		for (int p : node.getIR().getParameterValueNumbers()) {
			pkSet.add(new LocalPointerKey(node, p));
		}
		return pkSet;
	}

	public List<PointerKey> getAccessPath(PointerAnalysis pa,
			Set<PointerKey> pkSet, final PointerKey pk) {
		// TODO: Broken! Doesn't follow field accesses
		List<PointerKey> path = Lists.newArrayList();

		final Iterator<Object> iterator = (Iterator) pkSet.iterator();
		DFSPathFinder<Object> finder = DFSPathFinder.newDFSPathFinder(
				pa.getHeapGraph(), iterator, new Filter<Object>() {
					public boolean accepts(Object o) {
						return (pk.equals(o));
					}
				});
		List<Object> result = finder.find();
		if (result == null)
			return null;
		for (Object step : result) {
			if (step instanceof PointerKey) {
				path.add((PointerKey) step);
			}
		}
		return path;
	}

	public List<SSAInstruction> compileFlowMap(
			CGAnalysisContext<IExplodedBasicBlock> ctx,
			IMethod method,
			Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> flowMap) {
		final List<SSAInstruction> insts = Lists.newArrayList();
		// keep track of which SSA values have already been added to the result
		// list, and so can be referenced by subsequent instructions
		final BitSet refInScope = new BitSet();
		for (Entry<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> entry : flowMap
				.entrySet()) {
			final Pair<List<SSAInstruction>, Integer> lhs = compileFlowType(
					ctx, method, entry.getKey(), refInScope);
			insts.addAll(lhs.fst);
			for (FlowType<IExplodedBasicBlock> flow : entry.getValue()) {
				insts.addAll(compileFlowType(ctx, method, flow, refInScope,
						lhs.snd).fst);
			}
		}
		logger.debug("compiled flowMap: " + insts.toString());
		return insts;

	}

	private Pair<List<SSAInstruction>, Integer> compileFlowType(
			CGAnalysisContext<IExplodedBasicBlock> ctx, final IMethod method,
			final FlowType<IExplodedBasicBlock> ft, final BitSet refInScope) {
		return compileFlowType(ctx, method, ft, refInScope, -1);
	}

	/**
	 * Compile a FlowType into a list of SSA instructions representing that
	 * flow. If walking backwards from the FlowType's enclosed SSA instruction
	 * does not reach the lhsVal normally, we emit extra instructions to
	 * complete the chain, even if they do not yield type-correct code. If
	 * called on the LHS of a mapping, the lhsVal with be -1.
	 * 
	 * We accomplish this by checking whether the chain is complete after each
	 * recursive call to the PathWalker. If it happens to not be complete, we
	 * ignore the SSA vals that are actually there, eg, the ref field of a field
	 * get, and use the val from the LHS instead.
	 * 
	 * @param method
	 * @param flow
	 * @param refInScope
	 * @param lhsVal
	 * @return
	 */
	private Pair<List<SSAInstruction>, Integer> compileFlowType(
			CGAnalysisContext<IExplodedBasicBlock> ctx, final IMethod method,
			final FlowType<IExplodedBasicBlock> flow, final BitSet refInScope,
			final int lhsVal) {
		// what's the largest SSA value that refers to a parameter?
		final int maxParam = method.getNumberOfParameters();
		// set the implicit values for parameters
		refInScope.set(1, maxParam + 1);

		final CGNode node = ctx.nodeForMethod(method);
		final DefUse du = node.getDU();
		final SSAInstructionFactory instFactory = new JavaLanguage()
				.instructionFactory();
		final ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> graph = ctx.graph;

		final List<SSAInstruction> insts = Lists.newArrayList();
		// in case order matters, add any return statements to this list, to be
		// combined at the end
		final List<SSAInstruction> returns = Lists.newArrayList();
		Integer val = flow
				.visit(new FlowType.FlowTypeVisitor<IExplodedBasicBlock, Integer>() {
					final class PathWalker extends
							ThrowingSSAInstructionVisitor {
						public boolean completedChain = false || lhsVal == -1;

						public PathWalker() {
							super(new IllegalArgumentException(
									"unhandled SSAInstruction"));
						}

						@Override
						public void visitArrayLoad(
								SSAArrayLoadInstruction instruction) {
							// final int ref = instruction.getArrayRef();
							// if (ref != -1 && !refInScope.get(ref)) {
							// // ref is not in scope yet, so find the SSA
							// // instruction that brings it into scope
							// SSAInstruction refInst = du.getDef(ref);
							// if (refInst != null) {
							// refInst.visit(this);
							// completedChain = completedChain
							// || ref == lhsVal;
							// // postcondition: ref is now in scope
							// ;
							// assert ref == -1 || refInScope.get(ref)
							// || !completedChain;
							// }
							// }
							//
							// final int def = instruction.getDef();
							// // since wala can't read arrayload tags in
							// // summaries, just turn this into a checked cast
							// // (sloppy...)
							// SSAInstruction newInstruction = instFactory
							// .CheckCastInstruction(def,
							// completedChain ? ref : lhsVal,
							// instruction.getElementType(), true);
							//
							// // if this val is already in scope, don't emit
							// more
							// // instructions
							// if (refInScope.get(def)) {
							// return;
							// } else {
							// insts.add(newInstruction);
							// refInScope.set(def);
							// }
						}

						@Override
						public void visitGet(SSAGetInstruction instruction) {
							final int ref = instruction.getRef();
							if (ref != -1 && !refInScope.get(ref)) {
								// ref is not in scope yet, so find the SSA
								// instruction that brings it into scope
								SSAInstruction refInst = du.getDef(ref);
								if (refInst != null) {
									refInst.visit(this);
									completedChain = completedChain
											|| ref == lhsVal;
									// postcondition: ref is now in scope
									;
									assert ref == -1 || refInScope.get(ref)
											|| !completedChain;
								}
							}

							final int def = instruction.getDef();
							if (!completedChain) {
								if (!refInScope.get(lhsVal)) {
									du.getDef(lhsVal).visit(this);
									if (!completedChain) {
										logger.error("can't bring LHS into scope!");
									}
								}
								// if we haven't completed the chain by now,
								// stuff the lhsVal into the field
								FieldReference field = instruction
										.getDeclaredField();
								if (instruction.isStatic()) {
									logger.error("impossible");
								} else {
									instruction = instFactory.GetInstruction(
											def, lhsVal, field);
								}
							}

							// if this val is already in scope, don't emit more
							// instructions
							if (refInScope.get(def)) {
								return;
							} else {
								insts.add(instruction);
								refInScope.set(def);
							}
						}

						@Override
						public void visitPut(SSAPutInstruction instruction) {
							int ref = instruction.getRef();
							// a ref does not make a completed chain, since we
							// aren't getting the value from it
							if (ref != -1 && !refInScope.get(ref)) {
								// ref is not in scope yet, so find the SSA
								// instruction that brings it into scope
								SSAInstruction refInst = du.getDef(ref);
								assert refInst != null;
								refInst.visit(this);
							}
							// postcondition: ref is now in scope
							assert ref == -1 || refInScope.get(ref);

							int val = instruction.getVal();
							if (!refInScope.get(val)) {
								// if the RHS of the assignment is not in scope,
								// recur
								SSAInstruction valInst = du.getDef(val);
								if (valInst != null) {
									valInst.visit(this);
									// LHS flows into target field
									completedChain = completedChain
											|| val == lhsVal;
									// postcondition: val is now in scope
									assert refInScope.get(val)
											|| !completedChain;
								}
							}

							if (!completedChain) {
								if (!refInScope.get(lhsVal)) {
									du.getDef(lhsVal).visit(this);
									if (!completedChain) {
										logger.error("can't bring LHS into scope!");
									}
								}
								// if we haven't completed the chain by now,
								// stuff the lhsVal into the field
								FieldReference field = instruction
										.getDeclaredField();
								if (instruction.isStatic()) {
									instruction = instFactory.PutInstruction(
											lhsVal, field);
								} else {
									instruction = instFactory.PutInstruction(
											ref, lhsVal, field);
								}
							}
							insts.add(instruction);
						}

						@Override
						public void visitInvoke(SSAInvokeInstruction instruction) {

						}

						@Override
						public void visitNew(SSANewInstruction instruction) {

						}

						@Override
						public void visitReturn(SSAReturnInstruction instruction) {
							// returns only have a single use (-1 if void
							// return), so walk that val if present and then add
							// this
							// instruction to the return list
							int use = instruction.getUse(0);
							if (use != -1 && !refInScope.get(use)) {
								// use is not in scope yet
								SSAInstruction useInst = du.getDef(use);
								if (useInst != null) {
									completedChain = completedChain
											|| use == lhsVal;
									useInst.visit(this);
									// postcondition: use is now in scope, if
									// present
									assert (use == -1 || refInScope.get(use) || !completedChain);
								}

							}

							if (!completedChain) {
								// shove into return value if chain not
								// finished
								if (!refInScope.get(lhsVal)) {
									du.getDef(lhsVal).visit(this);
									if (!completedChain) {
										logger.error("can't bring LHS into scope!");
									}
								}
								instruction = instFactory.ReturnInstruction(
										lhsVal,
										instruction.returnsPrimitiveType());
							}
							returns.add(instruction);
						}

						@Override
						public void visitCheckCast(
								SSACheckCastInstruction instruction) {

						}

						@Override
						public void visitPhi(SSAPhiInstruction instruction) {

						}

					}

					@Override
					public Integer visitFieldFlow(
							FieldFlow<IExplodedBasicBlock> flow) {
						if (flow.getBlock().getLastInstructionIndex() != 0) {
							logger.warn("basic block with length other than 1: "
									+ flow.getBlock());
						}
						final SSAInstruction inst = flow.getBlock()
								.getLastInstruction();
						inst.visit(new PathWalker());
						return inst.getDef();
					}

					@Override
					public Integer visitIKFlow(IKFlow<IExplodedBasicBlock> flow) {
						IllegalArgumentException e = new IllegalArgumentException(
								"shouldn't find any IKFlows");
						logger.error("exception compiling FlowType", e);
						throw e;
					}

					@Override
					public Integer visitParameterFlow(
							ParameterFlow<IExplodedBasicBlock> flow) {
						// ParameterFlow can be used in two ways. Here we handle
						// the way
						// that references a parameter of the current method,
						// and do
						// nothing. The other way involves arguments to method
						// invocations, and AT says we shouldn't see any of
						// those
						// currently.

						// This loop detects the first case, where the block
						// associated
						// with the flow is equal to the entry block of the
						// method
						boolean equal = false;
						for (BasicBlockInContext<IExplodedBasicBlock> entryBlock : graph
								.getEntriesForProcedure(node)) {
							equal = equal || flow.getBlock().equals(entryBlock);
						}
						if (!equal) {
							IllegalArgumentException e = new IllegalArgumentException(
									"shouldn't have any ParameterFlows for invoked arguments");
							logger.error("exception compiling FlowType", e);
						}
						return Integer.valueOf(flow.getArgNum() + 1);
					}

					@Override
					public Integer visitReturnFlow(
							ReturnFlow<IExplodedBasicBlock> flow) {
						if (flow.getBlock().getLastInstructionIndex() != 0) {
							logger.warn("basic block with length other than 1: "
									+ flow.getBlock());
						}
						SSAInstruction inst = flow.getBlock()
								.getLastInstruction();
						// TODO: SUPPOSEDLY Two cases here:
						// 1. source == true: block should be an invoke
						// instruction
						// 2. source == false: block should be a return
						// instruction
						// handle both by invoking the PathWalker to ensure all
						// relevant
						// refs are in scope
						if (inst == null) {
							Iterator<BasicBlockInContext<IExplodedBasicBlock>> it = graph
									.getPredNodes(flow.getBlock());
							while (it.hasNext() && inst == null) {
								BasicBlockInContext<IExplodedBasicBlock> realBlock = it
										.next();
								inst = realBlock.getLastInstruction();
							}
						}
						inst.visit(new PathWalker());

						// final PointerKey pkFromFlowType = getPKFromFlowType(
						// method, flow);
						// logger.debug("ReturnFlow PK: " + pkFromFlowType);
						// logger.debug("Path from params: "
						// + getAccessPath(getInputPointerKeys(method),
						// pkFromFlowType));
						return Integer.valueOf(inst.getDef());
					}

					@Override
					public Integer visitStaticFieldFlow(
							StaticFieldFlow<IExplodedBasicBlock> flow) {
						SSAInstruction inst = flow.getBlock()
								.getLastInstruction();
						if (inst == null) {
							Iterator<BasicBlockInContext<IExplodedBasicBlock>> it = graph
									.getPredNodes(flow.getBlock());
							while (it.hasNext() && inst == null) {
								BasicBlockInContext<IExplodedBasicBlock> realBlock = it
										.next();
								inst = realBlock.getLastInstruction();
							}
						}
						inst.visit(new PathWalker());
						return Integer.valueOf(inst.getDef());
					}
				});
		insts.addAll(returns);
		return Pair.make(insts, val);
	}

	private PointerKey getPKFromFlowType(
			final CGAnalysisContext<IExplodedBasicBlock> cgContext,
			final IMethod method, FlowType<IExplodedBasicBlock> ft) {
		return ft.visit(new FlowTypeVisitor<IExplodedBasicBlock, PointerKey>() {
			final CGNode node = cgContext.nodeForMethod(method);

			@Override
			public PointerKey visitFieldFlow(FieldFlow<IExplodedBasicBlock> flow) {
				int val = flow.getBlock().getLastInstruction().getUse(0);

				// first look up the PK of the reference
				PointerKey instancePK = cgContext.pa.getHeapModel()
						.getPointerKeyForLocal(node, val);

				// then get IKs for this PK. under 0cfa, this should just be a
				// singleton
				OrdinalSet<InstanceKey> iks = cgContext.pa
						.getPointsToSet(instancePK);
				Iterator<InstanceKey> ikIter = iks.iterator();
				InstanceKey instanceIK = ikIter.next();
				// if there are any other candidates, warn
				if (ikIter.hasNext()) {
					logger.warn("found multiple IKs for a PK");
				}
				return cgContext.pa.getHeapModel()
						.getPointerKeyForInstanceField(instanceIK,
								flow.getField());
			}

			@Override
			public PointerKey visitIKFlow(IKFlow<IExplodedBasicBlock> flow) {
				throw new IllegalArgumentException("IKFlows not implemented");
			}

			@Override
			public PointerKey visitParameterFlow(
					ParameterFlow<IExplodedBasicBlock> flow) {
				// ParameterFlow can be used in two ways. Here we handle the way
				// that references a parameter of the current method, and do
				// nothing. The other way involves arguments to method
				// invocations, and AT says we shouldn't see any of those
				// currently.

				// This loop detects the first case, where the block associated
				// with the flow is equal to the entry block of the method
				boolean equal = false;
				for (BasicBlockInContext<IExplodedBasicBlock> entryBlock : cgContext.graph
						.getEntriesForProcedure(node)) {
					equal = equal || flow.getBlock().equals(entryBlock);
				}
				if (!equal) {
					IllegalArgumentException e = new IllegalArgumentException(
							"shouldn't have any ParameterFlows for invoked arguments");
					logger.error("exception compiling FlowType", e);
				}
				// +1 to get SSA val
				return cgContext.pa.getHeapModel().getPointerKeyForLocal(node,
						flow.getArgNum() + 1);
			}

			@Override
			public PointerKey visitReturnFlow(
					ReturnFlow<IExplodedBasicBlock> flow) {
				SSAInstruction inst = flow.getBlock().getLastInstruction();
				if (inst == null) {
					Iterator<BasicBlockInContext<IExplodedBasicBlock>> it = cgContext.graph
							.getPredNodes(flow.getBlock());
					if (it.hasNext()) {
						BasicBlockInContext<IExplodedBasicBlock> realBlock = it
								.next();
						inst = realBlock.getLastInstruction();
					} else {
						logger.error("synthetic return flow with no predecessor: probably shouldn't happen");
						throw new IllegalArgumentException();
					}
				}
				int val;
				// now we have to handle the two variants of this flow.
				if (flow.isSource()) {
					// If it's a source, then this represents the return value
					// of an
					// invoked method, so we use the getDef value.
					val = ((SSAInvokeInstruction) inst).getReturnValue(0);
				} else {
					// If it's a sink, then we use the getUse value.
					val = ((SSAReturnInstruction) inst).getResult();
					assert val != -1;
				}
				return cgContext.pa.getHeapModel().getPointerKeyForLocal(node,
						val);
			}

			@Override
			public PointerKey visitStaticFieldFlow(
					StaticFieldFlow<IExplodedBasicBlock> flow) {
				// static field access; easy
				return cgContext.pa.getHeapModel().getPointerKeyForStaticField(
						flow.getField());
			}
		});
	}

	/**
	 * A one-time-use context for a single method summarization. This manages
	 * various state built up by the summarization process such as the symbol
	 * table, maps from flows to SSA values, and the new (synthetic)
	 * instructions
	 * 
	 * @author acfoltzer
	 * 
	 */
	private static class MethodSummarizer {
		private static final Logger logger = LoggerFactory
				.getLogger(Summarizer.MethodSummarizer.class);
		private static final SSAInstructionFactory instFactory = Language.JAVA
				.instructionFactory();

		private final List<SSAInstruction> insts = Lists.newArrayList();
		private final Map<SSAInvokeInstruction, SSAInvokeInstruction> invs = Maps
				.newHashMap();
		private final Map<TypeReference, Integer> objs = Maps.newHashMap();
		private final CGAnalysisContext<IExplodedBasicBlock> ctx;
		private final IMethod method;
		private final SymbolTable tbl;

		/**
		 * Populated by compileSources
		 */
		private final Map<FlowType<IExplodedBasicBlock>, Integer> sourceMap = Maps
				.newHashMap();

		public MethodSummarizer(CGAnalysisContext<IExplodedBasicBlock> ctx,
				IMethod method) {
			this.ctx = ctx;
			this.method = method;

			final int numParams = method.getNumberOfParameters();
			// symbol table initially has parameter values reserved
			this.tbl = new SymbolTable(numParams);

			// if any params are reference types, we can use them for later
			// value resolution
			for (int param = 0; param < numParams; param++) {
				final TypeReference typeRef = method.getParameterType(param);
				if (!typeRef.isPrimitiveType()) {
					objs.put(typeRef, Integer.valueOf(param));
				}
			}
		}

		public List<SSAInstruction> summarizeFlows(
				Map<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> flowMap) {
			compileSources(flowMap.keySet());
			for (Entry<FlowType<IExplodedBasicBlock>, Set<FlowType<IExplodedBasicBlock>>> entry : flowMap
					.entrySet()) {
				FlowType<IExplodedBasicBlock> source = entry.getKey();
				for (FlowType<IExplodedBasicBlock> sink : entry.getValue()) {
					compileEdge(source, sink);
				}
			}
			return insts;
		}

		/**
		 * Given a set of sources (from the LHS of the flow map), create
		 * sufficient instructions to make SSA values for each source. After
		 * running this method, the instructions will be in insts, and sourceMap
		 * will contain the associated SSA values.
		 * 
		 * @param sources
		 */
		private void compileSources(Set<FlowType<IExplodedBasicBlock>> sources) {
			for (FlowType<IExplodedBasicBlock> source : sources) {
				source.visit(new FlowTypeVisitor<IExplodedBasicBlock, Void>() {

					@Override
					public Void visitFieldFlow(
							FieldFlow<IExplodedBasicBlock> flow) {
						// first create an object of the right type
						int ref = findOrCreateValue(flow.getField()
								.getDeclaringClass().getReference());
						// then deref the field
						int field = tbl.newSymbol();
						insts.add(instFactory.GetInstruction(field, ref, flow
								.getField().getReference()));
						// associate the dereffed value with this flow
						sourceMap.put(flow, Integer.valueOf(ref));
						return null;
					}

					@Override
					public Void visitIKFlow(IKFlow<IExplodedBasicBlock> flow) {
						// just create a new object of this type
						int ref = findOrCreateValue(flow.getIK()
								.getConcreteType().getReference());
						// associate the new object with this flow
						sourceMap.put(flow, Integer.valueOf(ref));
						return null;
					}

					@Override
					public Void visitParameterFlow(
							ParameterFlow<IExplodedBasicBlock> flow) {
						// two cases: either this is a formal to the method
						// we're analyzing, or an actual to a method that writes
						// to some fields on its corresponding formal.

						if (flow.getBlock().isEntryBlock()) {
							// In the first case, we just associate the val
							// number with this flow
							sourceMap.put(flow, Integer.valueOf(tbl
									.getParameter(flow.getArgNum())));
							return null;
						} else {
							// In the second case, we have to synthesize a call
							// to the function
							SSAInvokeInstruction inv = (SSAInvokeInstruction) flow
									.getBlock().getDelegate().getInstruction();
							SSAInvokeInstruction synthInv = findOrCreateInvoke(inv);
							sourceMap.put(flow, Integer.valueOf(synthInv
									.getUse(flow.getArgNum())));
						}
						return null;
					}

					@Override
					public Void visitReturnFlow(
							ReturnFlow<IExplodedBasicBlock> flow) {
						// this will only be the case where we have a flow from
						// the
						// result of an invoked method
						SSAInvokeInstruction inv = (SSAInvokeInstruction) flow
								.getBlock().getDelegate().getInstruction();
						SSAInvokeInstruction synthInv = findOrCreateInvoke(inv);
						sourceMap.put(flow, Integer.valueOf(synthInv.getDef()));
						return null;
					}

					@Override
					public Void visitStaticFieldFlow(
							StaticFieldFlow<IExplodedBasicBlock> staticFieldFlow) {
						// just create a static get instruction for this field
						int val = tbl.newSymbol();
						// TODO: this will create multiple get instructions if
						// more than one flow goes through a static field, but
						// the overhead isn't too bad
						insts.add(instFactory.GetInstruction(val,
								staticFieldFlow.getField().getReference()));
						return null;
					}

				});
			}
		}

		/**
		 * Given a source and a sink, create sufficient instructions to
		 * represent a flow from that source to that sink.
		 * 
		 * @param source
		 * @param sink
		 */
		private void compileEdge(FlowType<IExplodedBasicBlock> source,
				FlowType<IExplodedBasicBlock> sink) {
			final int sourceVal = sourceMap.get(source).intValue();
			sink.visit(new FlowTypeVisitor<IExplodedBasicBlock, Void>() {

				@Override
				public Void visitFieldFlow(FieldFlow<IExplodedBasicBlock> flow) {
					// first create an object to assign to
					int ref = findOrCreateValue(flow.getField()
							.getDeclaringClass().getReference());
					// then put sourceVal into the field
					insts.add(instFactory.PutInstruction(ref, sourceVal, flow
							.getField().getReference()));
					return null;
				}

				@Override
				public Void visitIKFlow(IKFlow<IExplodedBasicBlock> flow) {
					// TODO unused?
					throw new UnimplementedError("IKFlow as a sink?");
				}

				@Override
				public Void visitParameterFlow(
						ParameterFlow<IExplodedBasicBlock> flow) {
					// I don't think we need to do anything for the case where
					// this is a formal param of the summarized method
					if (flow.getBlock().isEntryBlock()) {
						return null;
					}

					// Otherwise we need to synthesize a call to the function
					// whose actual parameter is a sink.
					SSAInvokeInstruction inv = (SSAInvokeInstruction) flow.getBlock().getDelegate().getInstruction();
					findOrCreateInvoke(inv);
					return null;
				}

				@Override
				public Void visitReturnFlow(ReturnFlow<IExplodedBasicBlock> flow) {
					// recover return type
					TypeReference typeRef = method.getReturnType();
					// emit return instruction
					insts.add(instFactory.ReturnInstruction(sourceVal, typeRef.isPrimitiveType()));					
					return null;
				}

				@Override
				public Void visitStaticFieldFlow(
						StaticFieldFlow<IExplodedBasicBlock> flow) {
					// emit field put
					insts.add(instFactory.PutInstruction(sourceVal, flow.getField().getReference()));
					return null;
				}
			});
		}

		private SSAInvokeInstruction findOrCreateInvoke(SSAInvokeInstruction inv) {
			SSAInvokeInstruction synthInv = invs.get(inv);
			if (synthInv != null) {
				// return existing instruction if we already have it
				return synthInv;
			}

			final MethodReference declaredTarget = inv.getDeclaredTarget();
			final int numParams = declaredTarget.getNumberOfParameters();
			int[] paramVals = new int[numParams];
			for (int i = 0; i < numParams; i++) {
				TypeReference paramType = declaredTarget.getParameterType(i);
				if (paramType.isPrimitiveType()) {
					paramVals[i] = findOrCreateValue(paramType);
				} else {
					int ref = findOrCreateValue(paramType);
					paramVals[i] = ref;
				}
			}
			if (inv.hasDef()) {
				synthInv = instFactory.InvokeInstruction(inv.getDef(),
						paramVals, inv.getException(), inv.getCallSite());
			} else {
				synthInv = instFactory.InvokeInstruction(paramVals,
						inv.getException(), inv.getCallSite());
			}
			insts.add(synthInv);
			invs.put(inv, synthInv);
			return synthInv;
		}

		private int findOrCreateValue(TypeReference typeRef) {
			if (typeRef.isPrimitiveType()) {
				return findOrCreateConstant(typeRef);
			} else {
				return findOrCreateObject(typeRef);
			}
		}

		private int findOrCreateConstant(TypeReference paramType) {
			if (paramType.equals(TypeReference.Boolean)) {
				return tbl.getConstant(false);
			} else if (paramType.equals(TypeReference.Double)) {
				return tbl.getConstant(0d);
			} else if (paramType.equals(TypeReference.Float)) {
				return tbl.getConstant(0f);
			} else if (paramType.equals(TypeReference.Int)) {
				return tbl.getConstant(0);
			} else if (paramType.equals(TypeReference.Long)) {
				return tbl.getConstant(0l);
			} else if (paramType.equals(TypeReference.JavaLangString)) {
				return tbl.getConstant("");
			} else {
				logger.error("non-constant type reference {}", paramType);
				throw new RuntimeException();
			}
		}

		private int findOrCreateObject(TypeReference typeRef) {
			Integer objVal = objs.get(typeRef);
			if (objVal != null) {
				// return existing value if we already have one
				return objVal.intValue();
			}
			int ref = tbl.newSymbol();
			insts.add(instFactory.NewInstruction(ref, NewSiteReference.make(
					ProgramCounter.NO_SOURCE_LINE_NUMBER, typeRef)));
			objs.put(typeRef, Integer.valueOf(ref));
			return ref;
		}

	}
}
