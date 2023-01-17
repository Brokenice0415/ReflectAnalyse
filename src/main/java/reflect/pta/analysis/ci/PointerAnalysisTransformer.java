package reflect.pta.analysis.ci;

import reflect.cha.CallEdge;
import reflect.cha.JimpleCallGraph;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

/**
 * Created by liture on 2021/9/20 11:12 下午
 */
public class PointerAnalysisTransformer extends SceneTransformer {

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        List<SootMethod> entries = new LinkedList<>();
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            for (SootMethod method : clazz.getMethods()) {
                if ("main".equals(method.getName())) {
                    entries.add(method);
                }
            }
        }

        for (SootMethod entry : entries) {
            PointerAnalysis pointerAnalysis = new PointerAnalysis(entry);
            pointerAnalysis.solve();

            PointerFlowGraph PFG = pointerAnalysis.PFG;
            System.out.println("======== PFG ========");
            Set<Pointer> pointerSet = new LinkedHashSet<>();
            pointerSet.addAll(PFG.varMap.values());
            pointerSet.addAll(PFG.instanceFieldMap.values());
            for (Pointer pointer : pointerSet) {
                StringBuilder buff = new StringBuilder();
                if (((Var)pointer).getVariable() == null) continue;
                buff.append(pointer).append("\n");
                buff.append("\t pts: ").append(pointer.getPointsToSet()).append("\n");
                buff.append("\t edges: ").append(PFG.getSuccessorOf(pointer)).append("\n");
                System.out.println(buff);
            }
            System.out.println("======== End of PFG ========\n");
            JimpleCallGraph CG = pointerAnalysis.CG;
            Queue<SootMethod> methodQueue = new LinkedList<>();
            for (SootMethod entryMethod: CG.getEntryMethods()) {
                methodQueue.offer(entryMethod);
            }
            while (!methodQueue.isEmpty()) {
                SootMethod sm = methodQueue.poll();
                StringBuilder buff = new StringBuilder();
                buff.append(sm.getName());
                buff.append("\n\tedges: \n");
                for (CallEdge edge: CG.getCallOutOf(sm)) {
                    if (edge.getCallee() != null) {
                        methodQueue.offer(edge.getCallee());
                        buff.append("\t->\t" + edge.getCallee() + "\n");
                    }
                }
                System.out.println(buff);
            }
        }
    }
}
