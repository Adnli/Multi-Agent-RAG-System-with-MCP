package com.example.finnews.agent;

public interface Agent<I, O> {
    O handle(I input);
}
