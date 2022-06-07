class TrieNode:

  def __init__(self, value):
    self._value = value
    self._children = dict()
    self._m_children = 0
    self._n_complete = 0

  @staticmethod
  def root():
    return TrieNode('')

  def __len__(self):
    return self._m_children + self._n_complete

  def insert(self, text):
    if len(text) == 0:
      self._n_complete += 1
      return
    value = text[0]
    if value not in self._children.keys():
      self._children[value] = TrieNode(value)
    self._children[value].insert(text[1:])
    self._m_children += 1

  def _find(self, text):
    if len(text) == 0:
      return self
    value = text[0]
    if value not in self._children.keys():
      return None
    return self._children[value]._find(text[1:])

  def prefixes(self, prefix='', order='freq'):
    """
    Get a list of prefixes that continue to form at least two completions.

    :param prefix: Optional string to capture only prefixes that start with this.
    :param order: Sort order. Options are 'freq', 'alpha', or 'len'. Default is 'freq'.
    :return: List of prefixes.
    """
    prefix_node = self._find(prefix)
    if prefix_node is None:
      return []
    prefixes = prefix_node._prefixes(prefix[:-1])
    TrieNode._sort(prefixes, order)
    return prefixes

  def _prefixes(self, prefix):
    if len(self._children) == 0:
      return []
    if len(self._children) == 1:
      for child in self._children.values():
        return child._prefixes(prefix + self._value)
    prefixes = [(prefix + self._value, self._m_children)]
    for child in self._children.values():
      prefixes.extend(child._prefixes(prefix + self._value))
    return prefixes

  def completions(self, prefix='', order='freq'):
    prefix_node = self._find(prefix)
    if prefix_node is None:
      return []
    completions = prefix_node._completions(prefix[:-1])
    TrieNode._sort(completions, order)
    return completions

  def _completions(self, prefix):
    if self._n_complete > 0:
      completions = [(prefix + self._value, self._n_complete)]
    else:
      completions = list()
    for child in self._children.values():
      completions.extend(child._completions(prefix + self._value))
    return completions

  @staticmethod
  def _sort(a, order):
    if order == 'freq':
      a.sort(key=lambda s_n: (-s_n[1], s_n[0]))
    elif order == 'alpha':
      a.sort(key=lambda s_n: (s_n[0], -s_n[1]))
    elif order == 'len':
      a.sort(key=lambda s_n: (len(s_n[0]), s_n[0], -s_n[1]))
    else:
      raise ValueError('Unrecognized order: {}'.format(order))
