# What is this mod?

This is a clientside tool to inspect packets while in game, dumping the names and contents into chat. Hover over one of the messages in chat to see that packet's contents.

# How do I use it?

Mineshark can be enabled with the following in-game command:`/mineshark enable all`

And that's it. However this will blow up your chat with all packets both incoming and outgoing. You may want more fine-grained inspection.

*Though, it'll blow up your chat anyway. There's a lot of network activity in this game.*

`/mineshark enable in` will only show incoming packets. `disable` turns it off.

`/mineshark enable out` will only show outgoing packets. `disable` turns it off.

`/mineshark filter <>` allows for regex filtering of packet class names. Wrap your regex in quotes as MC gets unhappy with raw asterisks. Be aware that these are *class* names, so all filtering will be the english names. Sorry other languages but I dunno how to include everybody for that. 

`/mineshark reset` Disables chat dumping and resets the filter

`/mineshark mode <>` Has `include` and `exclude` modes. This tells the filter to either be a whiteilst or blacklist.

# Does this affect packet contents?

No, this mod does not actually change anything about packets. It only displays them.

# Does this mod improve performance?

LOL no. It is not a performance mod. This is purely for information, it is guaranteed to slow down both the network thread and render threads from having to do extra work.

# Wait, but doesn't that make it unstable?

It shouldn't, but I suppose it would be possible for the CPU to not keep up but... if you have that much packet activity, something else is wrong.

At the end of the day, you should only have this mod enabled while you're actually learning about these packets. Either remove it or disable it when you aren't.

# Can I use it in a modpack?

You can, but you probably shouldn't. I have included a warning on mod construction to indicate this.

# Other loaders/versions?
I plan to go down to 1.20.6, but as for other loaders I'm lazy. Feel free to PR.

