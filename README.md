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

# Differences between JSLT and JSLT2
* Allow for including null or empty nodes - which gives a nice performance boost (~5%):
```java
Jslt2 runtime = Jslt2.builder().includeNulls(true).build();
```
* Currently no function parameter checks - this is considered a bug in JSLT2 
* Performance has been interesting.  I've tested on AMD Phenom II and Intel i5; on Intel, JSLT2 can be roughly 5% to 10% faster; and on AMD, JSLT2 is consistently 5%-10% *slower*.  To date, depending on the template the original JSLT code will be generally slightly faster than JSLT2 code.
