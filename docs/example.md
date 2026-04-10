
<a name="example-top"></a>

# Example of Simian execution

See an example of the commandline execution below:


```

java -jar simian-4.0.0.jar -includes="../code-repository/**/*.ext" 
                           -ignoreSubtypeNames -ignoreCurlyBraces
                           -ignoreLiterals -language=Java -reportDuplicateText 
                           -threshold=5 > simian_test_result.txt

```

In this example, we assume the source code is stored under the path "../code-repository/".

The file extension included in the compare is "\*.ext", for example, we can use "\*.c"

Also, the following parameters are used.

```

-ignoreSubtypeNames 
 
-ignoreCurlyBraces 

-ignoreLiterals 

-language=Java 

-reportDuplicateText

-threshold=5


```

Please note that most of the option are language dependant, meaning some of the option may not change the result in certain language.

For eaxmple, *-ignoreSubtypeNames* only work in Java, C, Groovy, and only works with the Subtype of those language.

Simian team is working to expend the language coverage of the process options.


After the execution, the result is stored in “simian_test_result.txt”

Please refer to the main document for the complete execution parameters and their meaning. (<a href="../README.md">Return to main document</a>)


