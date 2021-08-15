#AIC2021 ForcefullyVaxxedKids
## By Javier Nistal (JavierN) & Vicent Baeza (Visi)

This is the repository our team used during AIC2021, a 3-week competition about programming bots to play a certain game against other teams' bots. More info [here](https://coliseum.ai/home?lang=en&tournament=aic2021 "AIC2021's Homepage"). Since we didn't intend to publish the bot, so we apologize in advance for the lack of 'cleanliness' of some parts of the code.

Here we explain the development and evolution of our bots during the competition as well as our view on the game's meta over time.

## Initial meta

When the game first released, technology was extremely cheap. It was possible to win consistently before round 700 with rather simple bots. From our point of view, this left only 2 viable strategies that would have a chance of beating simple tech-rushers: a rush (going to the enemy base and killing it to win by destruction) and a tech-rush of our own. Any other strategy that did anything else would risk losing against a pure techrush because it wouldn't have enough time to achieve anything before the other team researched the wheel.

Bots based on rushing the enemy base achieved their objective spawning axemen that would gather around the enemy base to form a coordinated attack at a predefined round. It's success depended on several factors:
 - The map had to be favourable. otherwise the explorer and/or the axemen might not be able to reach the enemy base.
 - The defense of the enemy's bot. Defeating the defense (if any) was crucial, as if the attack failed you were almost guaranteed to lose (although almost no-one defended themselves, since that would mean that you would lose against tech-rushers that didn't).
 - Getting the timings correct: a premature attack round would result in insufficient axemen arriving to the enemy base, but a tardy one would give the enemy team enough leeway to research the wheel before their base was destroyed.

Bots based on tech rush had a few choices to make too:
 - Worker usage. If workers were used, then the resources that they gathered resouces might come in too late (or not come at all) and not be worth the investment.
 - Resource building placement. Placing the buildings in sheltered or remote areas was crucial, to avoid the enemy destroying them with workers/axemen.

In general, a well done rush with sufficient pathfinding and a good attack round could beat most tech-rushes consistently. However, if the techrush defended itself or the map was unfavourable, then defeat was almost guaranteed.
Techrushes, on the other hand, were consistent on most maps and against most enemies, if they were fast enough. In our case, most rushes were too slow to destroy our base before we researched The Wheel.

## Post balance patch bots

Since fairly simple tech-rushers were dominating the meta with the occasional map-dependent rush, the devs released a balance patch. The most important changes were:
 - Tech was now significantly more expensive. Many technologies were up to 3 times more expensive than in the previous version.
 - Workers and trappers had their hp reduced drastically but were made much cheaper to produce.
 - Resource buildings' production cost went up by 50%, and the tech that unlocked them (JOBS) also increased steeply in price.

With these changes, tech-rushers were now slower and required resouce buildings to be achieved in a reasonable time. This meant that gathering resources became vital to the tech meta, as now you had the time to make the investment in workers worth it.

As a consequence to this, rushers became more prevalent, as now they were comparatively much stronger than before. However, their main weaknesses were still as present as ever, and this prevented them from really becoming mainstream. However, a new strategy became viable: disruption. The point behind this strategy was to send a few units that would disrupt the enemy, destroying the units & buildings that they built while we researched, unbothered. This was achieved mainly by sending spearmen without torches, so that they could snipe anything that was lighted-up without being seen. However, this strategy was completely countered by if the enemy produced wolves, but since wolves didn't have any other use-case and were quite useless for rushing or disrupting, almost no-one used them. This strategy had most of the pros of both rushing and tech-rushing, while having comparatively fewer drawbacks. The only major drawback is that if the spearmen didn't reach their target, you could lose to a decent tech-rush.

The meta remained fairly static for the next week and a half: our 'disruption' strategy on top, distantly followed by some rushers and well-executed tech-rushers. This led to us winning both the sprint tournament and the Eduard Khill Tournament because we could focus on improving our already successful bot, making it robust against all but the harshest maps. 

## Last week meta

As expected, the disruption strategy was also adopted and improved-upon by other teams. Wolves were used to quickly kill the spearmen before they could do any damage and hunt the enemy workers down to prevent them from being cost-effective. This made wolves very hard to fight, since axemen weren't a good counter to them. Since wolves attack every turn, with them microing isn't really that important. Instead, target management & positioning became key, as utilizing their speed and stealth effectively made them outstandingly powerful at dealing with almost anything.

The meta shifted towards delaying jobs in order to spawn several offensive units and gain control of the map. Whoever got control of the map could build undisturbed, and would eventually win. This soon started to affect the initial phase of a game, leading to the mass use of workers in combat. Workers do not need technology, have a high DPS, and they could be spammed with minimal resource consumption. Overwelming the enemy with just workers, and preventing them from gathering resources became a staple of the meta. Workers were also decent against wolves, since they could run away from them while attacking them from outside the wolf's range, significantly damaging or even destroying them in the process.

## Characterics of our bot

Our bot was mostly adapted to the meta throughout the competition. During the last week, it had some distinguishing characteristics:
 - We spawned a couple of trappers to trap in a certain pattern. This made it trivial for our units to avoid, but resulted in some of the opponent's units (especially wolves) dying to traps. Since trappers were so cheap, killing even a single unit made all that we produced worth the investment. Although they didn't usually decide the outcome of the match, they did give us an edge in most occasions.
 - Our communications depended on both the round and the offset of the map. This meant that if someone copied messages from a previous match and tried to broadcast them during to confuse our bot, they would most likely fail and be safely ignored.
 - After researching jobs, we stopped researching unless we had enough to research everything up to the wheel or the match was going to end. This ensured that our spending of resources was optimal, since it allowed us to test every combination of technologies and choose the best one.

The complete source-code can be found under the 'finalBot' folder.