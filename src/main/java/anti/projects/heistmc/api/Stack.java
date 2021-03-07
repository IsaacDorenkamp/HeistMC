package anti.projects.heistmc.api;

import java.util.ArrayList;

public class Stack<T> {
  private ArrayList<T> array;
  public Stack(ArrayList<T> initial) {
    this.array = initial;
  }
  public Stack() {
    this(new ArrayList<T>());
  }
  
  public void push(T next) {
    array.add(next);
  }
  
  public T pop() {
    return array.remove(array.size() - 1);
  }
  
  public int size() {
    return array.size();
  }
}
