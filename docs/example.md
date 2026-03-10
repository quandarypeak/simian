
<a name="example-top"></a>

# Example of Simian execution

See an example of the commandline execution below:


```
java -jar simian-4.0.0.jar -includes="../code-repository/**/*.ext" -ignoreSubtypeNames -ignoreCurlyBraces -ignoreLiterals -language=Java -reportDuplicateText -threshold=5 > simian_test_result.txt
```

In this example, we assume the source code is stored under the path "../code-repository/".

The file extension included in the compare is "*.ext", for example, we can use "*.c"

Also, the following parameters are used.

-ignoreSubtypeNames 
-ignoreCurlyBraces 
-ignoreLiterals 
-language=Java 
-reportDuplicateText
-threshold=5

After the execution, the result is stored in “simian_test_result.txt”

The complete execution parameters and their meaning can be found on our website. (https://simian.quandarypeak.com/docs/)


