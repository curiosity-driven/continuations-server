var number;
do {

	number = read("Enter a number");

} while(isNaN(parseInt(number, 10)));

number + " is a number.";