# jslt2
VM based implementation of [JSLT](https://github.com/schibsted/jslt) - which is a JSON query and transformation language.

# Example Translations

Given input:

```json
{
	"name" : "Brett Favre",
	"team" : "Green Bay Packers"
}
```
and given the JSLT script:

```
{
	"player" : .name + " played for " + .team
}
```
outputs:

```json
{
	"player" : "Brett Favre played for Green Bay Packers"
}
```

# Implemented
* All language features 
* The standard library 
* Macro support
* The JSLT test suite - 99% of tests pass with the exception of the mentioned differences 


# How to use


Include in your Maven/Gradle project:

```xml
<!-- https://mvnrepository.com/artifact/com.github.tonysparks.jslt2/jslt2 -->
<dependency>
    <groupId>com.github.tonysparks.jslt2</groupId>
    <artifactId>jslt2</artifactId>
    <version>0.2.1</version>
</dependency>
```

How to initialize and evaluate a template expression:

```java
// use out of the box defaults
Jslt2 runtime = new Jslt2(); 

// or, customize the runtime...

Jslt2 runtime = Jslt2.builder()
            .resourceResolver(ResourceResolvers.newFilePathResolver(new File("./examples")))
            .enableDebugMode(true)
            .objectMapper(new ObjectMapper())
            .maxStackSize(1024 * 5)
            .build();
            
            
// Execute a template:
JsonNode input = ...; // some input Json
JsonNode result = runtime.eval("{ \"input\" : . }", input);
            
            
// or, compile a template for repeated use:
Template template = runtime.compile("{ \"input\" : . }");

JsonNode result1 = template.eval(input);
JsonNode input2 = ...; // different json input
JsonNode result2 = template.eval(input2);

```



# Differences between JSLT and JSLT2
* Allow for including null or empty nodes - which gives a nice performance boost (~5%):
```java
Jslt2 runtime = Jslt2.builder().includeNulls(true).build();
```
* Currently no function parameter checks - this is considered a bug in JSLT2 
* Allows block comments via the `/*` and `*/` syntax 
* Verbatim strings: 

```
let x = """this
   is a "verbatim"
   string"""
```

* Performance has been interesting.  I've tested on AMD Phenom II and Intel i5; on Intel, JSLT2 can be roughly 5% to 10% faster; and on AMD, JSLT2 is consistently 5%-10% *slower*.  To date, depending on the template the original JSLT code will be generally slightly faster than JSLT2 code.
* `async` keyword allows for running an expression in a background thread.  Once all background expressions have been evaluated, the final result is computed.
NOTE: This is currently an experimental feature and is limited to *object expression values*.  As an example:

```
// runs the makeDatabaseQuery functions in background threads, which allows them to be computed
// in parrallel 
{
  "a" : async makeDatabaseQuery(.someParam),     
  "b" : async makeDatabaseQuery(.someOtherParam),
}
```

Other expressions are NOT supported:

```
[for(.) async .] // NOT supported
{for(.) async .} // NOT supported
async method()   // NOT supported
[ async 1 ]      // NOT supported
let x = async 1  // NOT supported

// only object expression values are supported:
{
   "a": async .,    // Valid
   "b": async true, // Valid
   "c": async 1,    // Valid
   "d": async "string", // Valid (any expression is valid here!)
   if (.) "e" else "f": async "yo" // Valid, however due to the key being unknown upfront, async is ignored
}
```

You can customize the `ExecutorService` provided to the `Jslt2` runtime.  The default `ExecutorService` uses daemon threads and `Executors.newCachedThreadPool`.

```java
// Customize (or use an already created instance) of ExecutorService
ExecutorService service = ...
Jslt2 runtime = Jslt2.builder()
    .executorService(service)    
    .build();
```