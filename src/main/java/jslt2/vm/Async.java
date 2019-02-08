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
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        ObjectNode object;
        String key;
        
        Task(Future<JsonNode> future, ObjectNode object, String key) {
            this.future = future;
            this.object = object;
            this.key = key;
        }
        
        void await() {            
            try {
                JsonNode value = future.get();
                if(value != null) {
                    if(this.object != null) {
                        this.object.set(this.key, value);
                    }
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
            VM vm = new VM(runtime, code.maxstacksize);
            JsonNode value = vm.execute(code, input);
            return value;
        });
    }
    

    public void submit(ObjectNode object, String key, JsonNode input, Bytecode code) {
        Future<JsonNode> future = submitTask(input, code);
        
        this.pendingTasks.add(new Task(future, object, key));
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
