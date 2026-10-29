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

// Test getter/setter accessors
class Temperature {
    private _celsius: number;

    constructor(celsius: number) {
        this._celsius = celsius;
    }

    get celsius(): number {
        return this._celsius;
    }

    set celsius(value: number) {
        this._celsius = value;
    }

    get fahrenheit(): number {
        return this._celsius * 9 / 5 + 32;
    }
}

// Test interfaces (TypeScript-only - no JavaScript equivalent)
interface Shape {
    readonly name: string;
    area(): number;
}

// Test enums (TypeScript-only - no JavaScript equivalent)
enum Color {
    Red,
    Green,
    Blue
}

// Test type aliases and generics (TypeScript-only - no JavaScript equivalent)
type Pair<T> = [T, T];

function firstOf<T>(pair: Pair<T>): T {
    return pair[0];
}

// Test destructuring and spread
const { key: destructuredKeyTypeScript } = objectTypeScript;
const combinedArrayTypeScript: number[] = [...arrayTypeScript, 4, 5];

// Test template literals with multiple interpolations
const summaryTypeScript: string = `${testTypeScriptFunction(1, 2)} and ${destructuredKeyTypeScript}`;