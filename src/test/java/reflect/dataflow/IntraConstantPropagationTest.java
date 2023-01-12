package reflect.dataflow;

import reflect.IntraBaseTest;
import soot.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by liture on 2021/9/19 4:29 下午
 */
public class IntraConstantPropagationTest extends IntraBaseTest {

    @Override
    public List<String> getProcessDirs() {
        return Arrays.asList("target/test-classes/");
    }

    @Override
    public List<String> getExcluded() {
        List<String> excluded = new LinkedList<>(super.getExcluded());
        excluded.add("untest.*");
        return excluded;
    }

    @Override
    public String getPhaseNameOfPack() {
        return "jtp";
    }

    @Override
    public String getPhaseNameOfTransformer() {
        return "jtp.intra_cp";
    }

    @Override
    public Transformer getTransformer() {
        return new IntraCPTransformer();
    }
}
