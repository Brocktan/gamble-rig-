# Fake Dispenser (Fabric, MC 1.21.11, client-side)
Look at a dispenser, press **K**, set item / name / count / enchants, hit Save.
Items from that dispenser look like your chosen item (name + enchants) on the ground AND in your inventory/hand, on YOUR screen only.
Settings: `config/fakedispenser.json`.
Build: push to GitHub -> Actions -> download the `fakedispenser-mod` artifact.

Keys (Controls > Misc): K = open menu while looking at a dispenser, J = toggle the whole mod on/off (silent).
Inside a configured dispenser's GUI there is a button to toggle fake items for that dispenser.

Multiple items: in the menu use the numbered tabs and "+" to add up to 6 items per dispenser; each time the dispenser fires one is picked at random (2 items = 50/50). Each item has its own name, count and enchants.
