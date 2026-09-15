// Test TypeScript parser with various language features
const testTypeScriptFunction = (param1: number, param2: number): number => {
    let result = param1 + param2;
    return result;
};

class TestTypeScriptClass {
    private name: string;

    constructor(name: string) {
        this.name = name;
    }

    public sayHello(): void {
        console.log(`Hello, ${this.name}!`);
    }
}

// Test different types of comments
/* This is a block comment
   spanning multiple lines */

// Test different data types
const numberTypeScript: number = 42;
const stringTypeScript: string = "Hello World";
const arrayTypeScript: number[] = [1, 2, 3];
const objectTypeScript: { key: string } = { key: "value" };

// Test control structures
if (number > 40) {
    console.log("Number is greater than 40");
} else {
    console.log("Number is less than or equal to 40");
}

for (let i: number = 0; i < array.length; i++) {
    console.log(array[i]);
}

// Test async/await
async function asyncTypeScriptTest(): Promise<any> {
    try {
        const response: Response = await fetch('https://api.example.com/data');
        const data: any = await response.json();
        return data;
    } catch (error: unknown) {
        console.error('Error:', error);
    }
}