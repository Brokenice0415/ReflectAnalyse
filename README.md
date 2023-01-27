# Reflection Analysis with Soot

## Preciseness

### Supported Rules:
- \[P-ForName\]
  - support
    1. `aClass = Class.forName("className")`
  - not support
    1. `aClass = Class.forName(str)` while `str` is a string variable.
    2. `aClass = A.class`
- \[P-GetMtd\]
  - support
    1. `method = aClass.getMethod("methodName")`
  - not support
    1. `method = aClass.getMethod(str)` while `str` is a string variable.
    2. `method = aClass.getMethod("methodName", Object[])`, which means this only support `void` functions.
    3. `method = aClass.getMethod(str, Object[])` of course.
- \[I-InvTp\]
  - support
    1. `method.invoke(instance)`
  - not support
    1. `method.invoke(instance, Object[])`

## How to Run&Test

### Dependency

- jdk1.8
- maven3.8.1 or higher
- soot4.2.1
- junit

### Test
IMPORTANT: \
target code: any `.java` in dir `src/test/testcodes/reflect`, user should mark this dir `Test Sources Root` by ideaJ.

then run `src/test/java/reflect/pta/PointerAnalysisTest.java` in ideaJ with Junit\
program will print PFG and CG of target code on terminal\

