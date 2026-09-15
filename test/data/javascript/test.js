// Test JavaScript parser with various language features
const testFunction = (param1, param2) => {
    let result = param1 + param2;
    return result;
};

class TestClass {
    constructor(name) {
        this.name = name;
    }

    sayHello() {
        console.log(`Hello, ${this.name}!`);
    }
}

// Test different types of comments
/* This is a block comment
   spanning multiple lines */

// Test different data types
const number = 42;
const string = "Hello World";
const array = [1, 2, 3];
const object = { key: "value" };

// Test control structures
if (number > 40) {
    console.log("Number is greater than 40");
} else {
    console.log("Number is less than or equal to 40");
}

for (let i = 0; i < array.length; i++) {
    console.log(array[i]);
}

// Test async/await
async function asyncTest() {
    try {
        const response = await fetch('https://api.example.com/data');
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Error:', error);
    }
} 