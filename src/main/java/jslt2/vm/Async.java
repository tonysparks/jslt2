/*
 * see license.txt
 */
package jslt2.vm;

import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;

/**
 * Asynchronous handling of tasks
 * 
 * @author Tony
 *
 */
public class Async {

    private Jslt2 runtime;
    
    static class Task {
        Future<JsonNode> future;
        final JsonNode[] stack;
        final int index;
        
        Task(Future<JsonNode> future, JsonNode[] stack, int index) {
            this.future = future;
            this.stack = stack;
            this.index = index;
        }
        
        void await() {            
            try {
                JsonNode value = future.get();
                if(value != null) {
                    this.stack[this.index] = value;
                }
            }
            catch(CancellationException | ExecutionException e) {
                throw new Jslt2Exception(e);
            }
            catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private Queue<Task> pendingTasks;
    
    /**
     * @param executorService
     */
    public Async(Jslt2 runtime) {
        this.runtime = runtime;
        
        this.pendingTasks = new ConcurrentLinkedQueue<>();
    }
    
    private Future<JsonNode> submitTask(JsonNode input, Bytecode code) {
        return this.runtime.getExecutorService().submit( () -> {
            VM vm = new VM(runtime);
            JsonNode value = vm.execute(code, input);
            return value;
        });
    }
    

    public void submit(JsonNode[] stack, int index, JsonNode input, Bytecode code) {
        Future<JsonNode> future = submitTask(input, code);
        
        this.pendingTasks.add(new Task(future, stack, index));
    }
    
    /**
     * Awaits for all the submitted tasks to be completed. 
     */
    public void await() {
        while(!this.pendingTasks.isEmpty()) {
            Task task = this.pendingTasks.poll();
            task.await();
        }
    }
}
