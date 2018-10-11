![alt text][logo]
# FSSync
FSSync is short for "Filesystems Synchronisator". The program is supposed to be a minimalistic data flow configurator to backup and archive files.
Yet it should be possible to map complex data structures.

## Getting Started
Just fetch the git repository and import the project path (.../fssync/fssync) as existing gradle project into eclipse. I hope that should do it.

### JDK Version
The target Java version is currently 1.8.

## Testing
There are no tests available yet.

## Deployment
When building the project an executable library file (.../fssync/fssync/build/libs/fssync-*version*.jar) is created containing all dependencies. This file can be used as stand alone runnable.
Also zip & tar archives are created (fssync/fssync/build/distributions/fssync.*version*.zip **or** .tar) these archives contain the runnable fssync.*version*.jar and its dependencies outside the archive.

There is no installer yet.

[logo]: fssync/src/main/resources/net/janbuchinger/code/fssync/res/fssyncLogo.png "FSSync Logo"
