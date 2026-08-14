# Contributing

Please remember to scrub output data from notebook cells.

Install the equivalent git filter:
`git config filter.strip-notebook-output.clean 'jupyter nbconvert --ClearOutputPreprocessor.enabled=True --to=notebook --stdin --stdout --log-level=ERROR'`
