# Summary
This is the official repository made to accompany the paper "Team 
Partitioning Approximation Algorithms".

## Dependencies
The TPP program relies on Google's OR Tools. Here is the link to the 
[OR-Tools API (Java)](https://or-tools.github.io/docs/javadoc/index.html).

To see the list of solver types, consult [this link](https://or-tools.github.io/docs/javadoc/com/google/ortools/linearsolver/MPSolver.html#createSolver-java.lang.String-).

## Running the Code
To run the code, you will need a properly formatted CSV file as input. In 
the `app` folder, you can find the relevant jar files and an accompanying 
csv formatted appropriately.

You can run the code by executing `app\run.bat` if you use Windows or 
`app/run.sh` if you use Linux or Mac. Your second argument should be the 
file path to the formatted csv. For example, on Windows and Linux respectively:

``.\app\run.bat .\app\example_format.csv``

``./app/run.sh ./app/example_format.csv``

To call the jar file via the command line, use 
the following command (assuming your pwd is the top-level folder of this 
repository):

``java -jar ./app/TeamSorting.jar <csv_file_path>``

### CSV Formatting
The `app/example_format.csv` contains a template for formatting the csv. The 
key parts of the template go down to rows 34. Below row 34, you can modify 
the csv without changing the result of the program. Below those lines, I 
have left some instructions on modifying the csv.

I've also left a `app/example_format.xlsx` file that contains some formulas 
for simple validation checking. Those formulas can tell you if the output 
would yield no solution even before running the linear program. It simply 
checks the total number of roles required by each team and the total number 
of preferences for each team.

### Random Input
If you want to run the code with random input, you can use the folowing 
command:  

``java -jar ./app/TeamSorting.jar r 25 5 3 3 1 2 4 5 1 2``

The numbers used in the command are the following arguments, in order:
- Number of members
- Number of teams
- Number of roles
- Number of preferences
- Lower bound for number of each role required by each team
- Upper bound for number of each role required by each team
- Lower bound for minimum member count per team
- Upper bound for minimum member count per team
- Lower bound for number of roles each member can fulfill
- Upper bound for number of roles each member can fulfill

## Modifying the Code


