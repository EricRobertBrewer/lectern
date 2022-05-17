def get_ticks(items, start=0, step=10):
  ticks = list()
  prev = None
  n = start
  for i, item in enumerate(items):
    if prev is None or prev != item:
      if n == 0:
        ticks.append((i, item))
        n = step - 1
      else:
        n -= 1
      prev = item
  return ticks
