# Manual micro-benchmark for large RecyclerView lists

This scenario verifies that list updates in adapters use diff-based updates and avoid full redraws.

## Preconditions

- Build type: `debug` (or `release` for cleaner measurements).
- Enable **Profile GPU rendering** and optionally **Show layout bounds** in Developer options.
- Use a dataset of at least:
  - Notes: 2,000 items
  - Subjects: 300 items
  - Homework rows (with headers): 2,000+ rows

## Scenario A: Cold open + fast scroll

1. Open screen with each list (`Notes`, `Subjects`, `Homework`).
2. Perform 3 fast top-to-bottom and bottom-to-top scroll passes.
3. Observe:
   - no mass blinking/flicker during bind;
   - no jumps to wrong rows;
   - stable item animations.

## Scenario B: Bulk content update (same IDs)

1. Update 10-20% of items with same IDs (title/color/status/description changes).
2. Re-submit updated list.
3. Expected:
   - only changed rows rebind/animate;
   - no full list flash (symptom of `notifyDataSetChanged()`-style update).

## Scenario C: Reorder/filter

1. Apply filter/search query reducing list to ~20-30%.
2. Clear filter back to full list.
3. Change sort order (e.g. due date/priority/name).
4. Expected:
   - smooth insert/remove/move animations;
   - no duplicated rows;
   - no wrong-content rows after animation completes.

## Scenario D: Subject dictionary refresh for homework

1. Open homework list with many rows.
2. Change several subject names/colors.
3. Trigger `setSubjects(...)`.
4. Expected:
   - only visible task rows update subject chip/name/color;
   - no full list invalidation artifacts;
   - headers remain stable.

## Optional instrumentation

- In Android Studio Profiler, track:
  - frame time spikes during update/filter;
  - allocations during repeated `submitList(...)`.
- In `logcat`, compare number of `onBindViewHolder` calls before/after change.

## Pass criteria

- No visual artifacts (flicker, duplicate, wrong row reuse).
- No full-list redraw behavior on partial updates.
- Scroll remains responsive on large datasets.
