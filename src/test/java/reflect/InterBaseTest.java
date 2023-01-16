package reflect;

import org.junit.Before;
import org.junit.Test;
import soot.*;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.options.Options;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liture on 2021/9/20 5:05 下午
 */
public abstract class InterBaseTest extends IntraBaseTest {

    @Override
    public void setOptions() {
        super.setOptions();
        List<String> toInclude = new ArrayList<>();
        toInclude.add("java.lang.StringBuilder");
        toInclude.add("sun.reflect.Reflection");
        Options.v().set_include(toInclude);
//        Options.v().set_include_all(true);
    }

    @Override
    @Before
    public void initializeSoot() {
        this.setOptions();
        // 加载所有类
        Scene.v().loadNecessaryClasses();
    }

    @Test
    @Override
    public void test() {
        // user-defined transform
        String packPhaseName = getPhaseNameOfPack();
        String transformerPhaseName = getPhaseNameOfTransformer();
        Transformer transformer = getTransformer();
        Transform transform = new Transform(transformerPhaseName, transformer);
        PackManager.v().getPack(packPhaseName).add(transform);
        PackManager.v().getPack(packPhaseName).apply();
    }
}
