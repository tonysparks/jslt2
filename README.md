# jslt2
VM based implementation of [JSLT](https://github.com/schibsted/jslt)

# Implemented
* All language features 

# Road Map
* Implement all of the standard library - slowly chipping away at this
* Migrate over the JSLT test cases - in progress


# How to use

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
