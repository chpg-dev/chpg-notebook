# Contributing

Please remember to scrub output data from notebook cells.

Install the equivalent git filter:
`git config filter.strip-notebook-output.clean 'jupyter nbconvert --ClearOutputPreprocessor.enabled=True --to=notebook --stdin --stdout --log-level=ERROR'`

or run a clear command manually:
`python -m nbconvert --clear-output --inplace .\GettingStarted.ipynb`

# Bumping dependencies

To bump `pg` and `pgv` dependencies to the latest commits run `git submodule update --remote --recursive`
