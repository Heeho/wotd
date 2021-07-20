package ru.ltow.wotd;

import java.util.concurrent.ConcurrentHashMap;

public class Networker {
  private static ConcurrentHashMap<Class<? extends Object>, Object> socket;

  public <T> T receive(Class<T> type) {
    check();
    return (T) (socket.remove(type));
  }

  public <T> void send(Class<T> type, T data) {
    check();
    socket.put(type, data);
  }

  private void check() {
     if(socket == null) socket = new ConcurrentHashMap<>();
  }
}