public interface ImmutableQueue<T> {
    ImmutableQueue<T> enQueue(T t);
    ImmutableQueue<T> deQueue();
    T head();
    boolean isEmpty();
}
