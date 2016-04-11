
# java-for-playwrights

Protocol composition framework and embedded networking DSL (domain specific language) for Java 8.

# About this work

This is the partial result of my personal work for my master's degree (not attained so far). It includes a radical abstraction of network interface which seeks making code dependent on any kind of network behavior, from cryptographic protocols to parallel algorithms, more reusable. As so, it opens the doors for protocol composition, that is, treating protocols as if they were LEGO blocks. Thus, users can easily build their own custom communication stacks from scratch, taking only what they need.

The most interesting part, however, is the embedded domain specific language for protocol description. It is ultimately based on how we tell Alice-and-Bob stories. It allows a centralized and partially causal description of network behavior, with support to branching, looping and message loss provisioning. I have been able to code a some rudimentary, but already complex showcases in it, including a primitive transport protocol. 

Since the project has been growing in complexity and time to develop it has run short, I have decided to upload it on GitHub in an attempt to change its fate as academic abandoware. **If you are interested by it, please contact me!** I will be happy either to develop the ideas further or to pass the stick.

## State of development

The project is by no means in a mature state, but I think I can guarantee basic functionality. Particularly, the showcases supplied should work.

## Known issues and next steps

This is a quick list of the main known issues:

* When many different message combinations are possible for a next state, the `Actor` protocol engine chooses one at random (if I am not mistaken, the shortest one), when it should choose the longest. Intelligent protocol coding can work around this issue, but requires extra messages to be sent. To solve it, one needs only to implement an online critical path algorithm.
* Composing over connection oriented protocols is not yet tested.
* Missing support for multicast and broadcast. These will have to be done explicitly.
* More than one thread for `Character` is also not yet supported.
* Wrappers for TCP and UDP work, but the implementation is far from decent.
* Code is partially documented; a tutorial is lacking. Upload of the dissertation text is possible in the future.

And this is a list of future improvements:

* Allow protocols to invoke other protocols inside their description.
* Create a *standard library* with most commonly used protocols.
* Integrate with an SDN (Software Defined Network) NOS (Network Operational System). There are many security issues involved, but it would be an interesting follow-up project.
