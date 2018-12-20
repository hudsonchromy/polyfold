# define compiler flags
J_FLAGS = -g
JC = javac
J = java
MAIN = PolyFold

# clear default definitions
.SUFFIXES: .java .class

# build java files into class files
.java.class: 
	$(JC) $(J_FLAGS) $*.java

# set classes to track
CLASSES = \
	Controller.java \
	dihedralutils/DihedralUtility.java \
	PolyFold.java \
  Link.java

# default behavior
default: classes

# behavior for classes
classes: $(CLASSES:.java=.class)

# run after compile
run: classes 
	$(J) $(MAIN)

clean: 
	$(RM) *.class
	$(RM) ./dihedralutils/*.class
