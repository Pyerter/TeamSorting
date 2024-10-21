# Summary
This is the official repository made to accompany the paper "Team 
Partitioning Approximation Algorithms".

## Dependencies
The TPP program relies on Google's OR Tools. Here is the link to the 
[OR-Tools API (Java)](https://or-tools.github.io/docs/javadoc/index.html).

## Running the Code
To run the code, you will need a properly formatted CSV file as input. In 
the `app` folder, you can find the relevant jar files and an accompanying 
csv formatted appropriately. To call the jar file via the command line, use 
the following command (assuming your pwd is the top-level folder of this 
repository):

``java -jar ./app/TeamSorting.jar <csv_file_path>``

You can also run the batch file provided by simpling executing it 
`app/run.sh`. If you use Linux or Mac, you can use the 
`app/run.sh` file provided.

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

## Modifying the Code


