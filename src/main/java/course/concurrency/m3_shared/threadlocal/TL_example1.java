package course.concurrency.m3_shared.threadlocal;

public class TL_example1 {

    public static void main(String[] args) {
        Task task1 = new Task();

        new Thread(task1).start();
        new Thread(task1).start();
        new Thread(task1).start();
    }

    public static class Task implements Runnable {
        private static final ThreadLocal<Integer> VALUE = ThreadLocal.withInitial(() -> 0);

        @Override
        public void run() {
            Integer currentValue = VALUE.get();
            VALUE.set(currentValue + 1);
            System.out.print(VALUE.get() + " ");
        }
    }
}
