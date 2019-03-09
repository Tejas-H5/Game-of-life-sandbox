#Patterns.txt

In the scetch data folder, you will find a text file named "patterns.txt" that contains all of the patterns you can spawn at runtime.
Each line denotes a different pattern. It's not easy to add custom patterns at the moment, but you can do this by creating a new line, and then following this format:

```
...
Pattern name,x1,y1,x2,y2,...,xn,yn,n,patternWidth,patternHeight
...
```

`Pattern name` is the name of the pattern that appears when it is selected.
The numbers `x1...xn` and `y1...yn` are horizontal(x) and vertical(y) integer offsets from the cell at the mouse position for each pattern. The pattern should centered as such for UI elements to work.
`n` is the number of x-y pairs in total.
`patternWidth` and `patternHeight` are the pattern's width and height in cells. This info can be used when displaying the pattern inside UI elements. 

Maybe in the future I'll add an easy way of adding more patterns