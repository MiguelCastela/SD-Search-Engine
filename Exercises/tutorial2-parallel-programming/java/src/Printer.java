import java.util.Arrays;

public class Printer {
    public static void main(String[] args) {
        int[] array = new int[10];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }

        System.out.println("Serial execution");
        Arrays.stream(array)
                .forEach(v -> System.out.println(v));

        System.out.println("Parallel execution");
        Arrays.stream(array).parallel()
                .forEach(v -> System.out.println(v));
    }
}
