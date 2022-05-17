def get_general_conference_address_key(row):
  return '{}:{:d}'.format(row['conference'], row['ordinal'])
