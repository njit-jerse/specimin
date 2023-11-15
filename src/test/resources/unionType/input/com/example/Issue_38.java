

public class Issue_38 {

    static class ExampleClass {
        public ExampleClass(int value) {
            // Constructor with a parameter
        }
    }

    public void test() {
        try {
            // Attempting to create an instance of ExampleClass, which has no default constructor
            Class<?> exampleClass = ExampleClass.class;
            ExampleClass instance = (ExampleClass) exampleClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
